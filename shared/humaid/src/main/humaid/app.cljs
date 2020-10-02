(ns humaid.app
  (:require
   [ajax.core :refer [GET POST] :as ajax]
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [goog.string :as gstring]
   [goog.string.format]
   [humaid.ajax :as aj]
   [humaid.date :as date]
   [humaid.notification :as ntfc]
   [humaid.util :as u]
   [reagent-keybindings.keyboard :as kb]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [reitit.frontend :as rf]

   [clojure.spec.test.alpha :as stest]

   )
  (:import goog.History))


(goog-define API-ADDR "/api/v1")
(goog-define MKS-ADDR "")
(goog-define APP-PREFIX "/humaid")

(def delivery-item-categories {3 "Одежда"
                             17 "Гигиена"
                             22 "Костыли/трости"})

;; State
;; --------------------

(defonce state (r/atom {:page :start}))

(defonce stopwatch (r/atom {:val 0
                            :started false}))

;; Misc
;; --------------------

(defonce stopwatch-fn (atom nil))

(defn reset-stopwatch! []
  (js/clearInterval @stopwatch-fn)
  (swap! stopwatch assoc :val 0 :started false))

(defn start-stopwatch! []
  (when-not (:started @stopwatch)
    (swap! stopwatch assoc :started true)
    (reset! stopwatch-fn
            (js/setInterval
             (fn [] (swap! stopwatch update :val inc))
             1000))))

(defn set-hash! [loc]
  (set! (.-hash js/window.location) loc))

(defn client-fullname [client]
  (str (:lastname client) " " (:firstname client) " " (:middlename client)))

(defn client-photo [photo-name]
  ;; TODO: default photo
  (when photo-name
    (str MKS-ADDR "/uploads/images/client/photo/" (subs photo-name 0 2) "/" photo-name)))

(defn store-if-valid!
  ;; Returns funciton which validates `data` agaings `spec-key`.
  ;; It saves valid data in db under `store-key` or logs validation errors to console
  ;; and screams in panic.
  [store-key spec-key]
  (fn [data]
    (if (s/valid? spec-key data)
      (swap! state assoc store-key data)
      (do (prn "spec for" spec-key "failed: " (s/explain spec-key data))
          (ntfc/danger! (str "Ошибка валидации данных. spec-key: " spec-key))))))

;; Requests
;; --------------------

(def auth-interceptor
  (ajax/to-interceptor
   {:name "Auth interceptor"
    :response
    (fn [response]
      (if (= 401 (ajax.protocols/-status response))
        (do
          (let [login-addr  "/login?_target_path=/humaid/"]
            (set! (.-href js/window.location) login-addr))
          (reduced [0 nil]))
        response
        ))}))

(defn load-client! [id]
  (GET (str API-ADDR "/clients/" id)
       {:response-format (aj/json-resp (aj/key-fn-ns (namespace ::_)))
        :handler #(swap! state assoc :client %)
        :error-handler
        #(if (= 404 (:status %))
           (do (ntfc/danger! (gstring/format "Клиент (id = %d) не найден" id))
               (set-hash! "/"))
           (ntfc/danger! "Ошибка при загрузке клиента. Попробуйте перезагрузить страницу."))
        }))

(defn load-client-deliveries! [client-id]
  (GET (str API-ADDR "/clients/" client-id "/deliveries")
       {:response-format
        (aj/json-resp (merge (aj/key-fn-ns (namespace ::_))
                             {:transform-map
                              {::delivered-at date/str->date
                               ::delivery-item-id u/str->int
                               }}))
        :handler (store-if-valid! :client-deliveries ::deliveries)
        :error-handler
        #(if (= 404 (:status %))
           (do (ntfc/danger! (gstring/format "Клиент (id = %d) не найден" client-id))
               (set-hash! "/"))
           (ntfc/danger! "Ошибка при загрузке клиента. Попробуйте перезагрузить страницу."))
        }))

(defn load-client-services! [client-id]
  (GET (str API-ADDR "/clients/" client-id "/services")
       {:response-format (aj/json-resp (aj/key-fn-ns "service"))
        :vec-strategy :rails ;; https://cljdoc.org/d/cljs-ajax/cljs-ajax/0.8.0/api/ajax.url
        :params {:types (keys delivery-item-categories)}
        :handler #(swap! state assoc :client-services %)
        :error-handler
        #(if (= 404 (:status %))
           (do (ntfc/danger! (gstring/format "Клиент (id = %d) не найден" client-id))
               (set-hash! "/"))
           (ntfc/danger! "Ошибка при загрузке клиента. Попробуйте перезагрузить страницу."))
        }))

(defn load-delivery-items! []
  (GET (str API-ADDR "/delivery_items")
       {:response-format  (aj/json-resp (aj/key-fn-ns "delivery-item"))
        :handler #(swap! state assoc :humaid-items %)
        :error-handler
        #(ntfc/danger! "Ошибка при получении списка вещей. Попробуйте перезагрузить страницу.")
        }))

(defn save-items! [client-id item-ids]
  (POST (str API-ADDR "/clients/" client-id "/deliveries")
        {:params {"item_ids" @item-ids}
         :format :json
         :handler #(do
                     (ntfc/success! "Выдача сохранена.")
                     (reset! item-ids #{})
                     (set-hash! (str "/clients/" client-id)))
         :error-handler
         #(ntfc/danger! "Ошибка при сохранении выдачи. Попробуйте еще раз.")}))

(defn handle-search [search-res event]
  (let [name (-> event .-target .-value)]
    (if (empty? name)
      (do (reset-stopwatch!)
          (reset! search-res nil))
      (when (> (count name) 2)
        (do (start-stopwatch!)
            (GET (str API-ADDR "/clients/search")
                 {:params {:v name}
                  :response-format :json
                  :keywords? true
                  :handler #(reset! search-res %)
                  :error-handler
                  #(if (= 404 (:status %))
                     (reset! search-res [])
                     (ntfc/danger! "Ошибка при получении результатов поиска."))
                  }))))))

;; UI
;; --------------------

(defn stopwatch-ui []
  (let [v (:val @stopwatch)
        min  (quot v 60)
        sec  (rem v 60)]
    [:div
     {:class ["is-size-2" (when (> min 4) "has-text-warning")]}
     (gstring/format "%02d:%02d" min sec)]))

(defn modal-loading []
  [:div.modal.is-active
   [:div.modal-backgroud]
   [:div.modal-content
    [:div.content "Загружается..."]
    [:button.button.is-large.is-loading " -----------"]]])

(defn start-page []
  (r/with-let [search-res (r/atom nil)
               _  (reset-stopwatch!)]
    [:section.section>div.container>div.content
     [:section.columns
      [:div.column.is-2
       [stopwatch-ui]]

      (let [clients @search-res
            not-found (= clients [])]

        [:div.column
         [:h3 "Поиск клиента"]
         [:div.control
          [:input
           {:class ["input" "is-large" (when not-found "is-danger")]
            :type "text"
            :placeholder "ФИО..."
            :on-change #(handle-search search-res %)
            }]
          (when not-found [:p.help.is-danger "Не нашли =("])]

         (for [{:keys [id birthDate] :as client} clients
               :let [birth-date (date/date->yy-mm-dd (js/Date. birthDate))]]
           ^{:key id}
           [:div.is-size-2
            [:a {:href (str APP-PREFIX "/#/clients/" id)}
             (str (client-fullname client) " (" birth-date ")")]]
           )])]]))

(defn delivery-link [client-id kind]
  (str APP-PREFIX "/#/clients/" client-id "/delivery/" (name kind)))

(defn client-page [{{id :id} :path}]
  (r/with-let [_ (do (load-client! id)
                     (load-client-deliveries! id)
                     (load-client-services! id))
               _  (start-stopwatch!)]
    (if-let [client (:client @state)]
      [:section.section.container.content
       [:section.columns
        [:div.column.is-2
         [stopwatch-ui]
         [:button.button {:on-click #(set-hash! "/")} "Завершить выдачу"]]

        (let [clothes (delivery-link (:id client) :clothes)
              hygiene (delivery-link (:id client) :hygiene)
              crutches (delivery-link (:id client) :crutches)]
          [:div.column.is-3
           [:p>a {:href clothes} [:button.button.is-large.is-block "1. Одежда"]]
           [:p>a {:href hygiene} [:button.button.is-large.is-block "2. Гигиена"]]
           [:p>a {:href crutches}[:button.button.is-large.is-block "3. Костыли/трости"]]

           [kb/kb-action "1" #(set-hash! clothes)]
           [kb/kb-action "2" #(set-hash! hygiene)]
           [kb/kb-action "3" #(set-hash! crutches)]])

        [:div.column.is-3
         [:img
          {:src (client-photo (:photoName client))}]
         [:p.is-size-5 (client-fullname client)]]]]

      [modal-loading]
      )))

(defn redirect-modal [state]
  [:div.modal {:class [(when (:active? @state) "is-active")]}
   [:div.modal-background]
   [:div.modal-card
    [:div.modal-card-head]
    [:div.modal-card-body
     [:div.content "Есть несохранённые изменения. Продолжить?"]]
    [:div.modal-card-foot
     [:button.button {:on-click #(set-hash! (:url @state))} "Да"]
     [:button.button {:on-click #(swap! state assoc :active? false) :aria-label "close"} "Нет"]]
    ]])

(s/def ::delivered-at #(instance? js/Date %))
(s/def ::delivery-item-id pos-int?)
(s/def ::delivery (s/keys :req [::delivered-at ::delivery-item-id]))
(s/def ::deliveries (s/coll-of ::delivery))


(s/def ::id pos-int?)
(s/def ::limit-days nat-int?)
(s/def ::delivery-item (s/keys :req [::id ::limit-days]))

;; (s/def ::date #(instance? js/Date %))
;; (s/def ::nil nil?)
;; (s/fdef delivery-unavailable-until
;;   :args (s/cat ::deliveries ::delivery-item)
;;   :ret (s/nilable ::date))

(defn delivery-unavailable-until
  ;; deliveries -- list of items delivered to a client (list of maps with :deliveryItemID and :deliveredAt keys).
  ;; When current item (2nd arg) cannot be issued today returns nearest available date.
  [deliveries {item-id ::id limit-days ::limit-days} today]
  (prn "fofofo" deliveries item-id limit-days today)
  (when-let [item-delivery (some
                            #(when (= item-id (::delivery-item-id %)) %)
                            deliveries)]
    (prn "after when " item-delivery)
    (let [next-available-date (date/add-days (::delivered-at item-delivery) limit-days)]
      (when (< today next-available-date)
        next-available-date))))

;; (def res (let [deliveries [{::delivery-item-id 10 ::delivered-at (date/str->date "2020-08-17")}]
;;       delivery-item {::id 5 ::limit-days 5}
;;       today (date/today)]
;;          (delivery-unavailable-until deliveries delivery-item today)
;;   ))

(defn delivery-page [{{kind :kind id :id} :path}]
  (r/with-let [_ (start-stopwatch!)
               _ (when-not (:client @state)
                   (do (load-client! id)
                       (load-client-deliveries! id)
                       (load-client-services! id)))
               kind (keyword kind)
               hotkeys "1234567890qwertyuiopasdfghjklzxcvbnm[];',./"
               category-id (kind {:clothes 3
                                  :hygiene 17
                                  :crutches 22})
               heading (str "Выдача " (kind {:clothes "одежды"
                                             :hygiene "предметов гигиены"
                                             :crutches "костылей и тростей"}))
               selected-items (r/atom #{})
               switch-selected! (fn [selected-items item-id]
                                  (if (contains? @selected-items item-id)
                                    (swap! selected-items disj item-id)
                                    (swap! selected-items conj item-id)))

               redirect-modal-state (r/atom {:active? false :url nil})
               redirect! (fn [url selected-items]
                           (if (empty? @selected-items)
                             (set-hash! url)
                             (swap! redirect-modal-state assoc :active? true
                                    :url url)))]
    (if-let [client (:client @state)]

      [:section.section.container.content

       [redirect-modal redirect-modal-state]

       [:section.columns
        [:div.column.is-2
         [stopwatch-ui]
         [:p>button.button
          {:on-click #(redirect! (str "/clients/" id) selected-items)}
          "Вернуться к списку"]
         [:p>button.button.is-light.is-danger
          {:on-click #(redirect! "/" selected-items)}
          "Завершить выдачу"]]

        [:section.column
         (let [selected @selected-items
               items (filter #(= category-id (:category %)) (:humaid-items @state))
               deliveries (:client-deliveries @state)
               today (date/today)]
           [:div.container.content
            [:h3 heading]
            [:p.is-size-5.has-text-weight-light (str " " (:name client))]
            [:section

             (for [[{:keys [id name category limitDays] :as item} key] (map vector items (concat hotkeys (repeat nil)))
                   :let [unavailable-until (delivery-unavailable-until deliveries item today)
                         selected? (contains? selected id)]]
               [:<> {:key id}
                [:button
                 {:class ["button" "mx-2" "my-2" "disabled"
                          (when selected? "is-success")
                          (when (and unavailable-until (not selected?)) "is-light")]
                  :on-click #(switch-selected! selected-items id)}

                 (str (when key (str key ". ")) (clojure.string/capitalize name))
                 (when unavailable-until
                   [:span.has-text-weight-light.is-size-6.mx-1
                    (str "(доступно с " (date/date->mm-dd unavailable-until) ")")])]
                (when key
                  [kb/kb-action key #(switch-selected! selected-items id)])])]])

         (when-let [services (seq (filter #(= (str category-id) (:type %)) (:client-services @state)))]
           [:div.container.content
            [:h5 "Выдачи-услуги"]
            [:ul
             (for [{comment :comment created-at :createdAt} services
                   :let [created-at (date/date->yy-mm-dd (js/Date. created-at))]]
               [:li (str comment " (" created-at ")")])]])

         [:div.container.content
          [:button.button.is-light.is-success
           {:disabled (empty? @selected-items)
            :on-click #(save-items! (:id client) selected-items)}
           "Сохранить"]
            [kb/kb-action "ctrl-enter" #(save-items! (:id client) selected-items)]
          ]]]]

      [modal-loading]
      )))

;; -------------------------
;; Routing

(def pages
  {:start #'start-page
   :client #'client-page
   :delivery #'delivery-page})

(defn not-found-page []
  [:section.section>div.container>div.content
   [:p "404 (страница не найдена)"]
   [:p  [:a {:href "#/"} "назад"]]])

(defn page []
  (let [s @state]
    [:div
     [(pages (:page s) #'not-found-page) (:parameters s)]
     [kb/keyboard-listener]]))

(def router
  (rf/router
   [["/" :start]
    ["/clients/:id" :client]
    ["/clients/:id/delivery/:kind" :delivery]]))

(defn match-route [uri]
  (->> (or (not-empty (string/replace uri #"^.*#" "")) "/")
       (rf/match-by-path router)))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     HistoryEventType/NAVIGATE
     (fn [event]
       (let [m (match-route (.-token event))]
         (swap! state assoc
                :page (get-in m [:data :name])
                :parameters (:parameters m)))))
    (.setEnabled true)))

;; -------------------------

(defn mount-components []
  (rdom/render [#'ntfc/notification] (.getElementById js/document "notification"))
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (swap! ajax/default-interceptors concat [auth-interceptor])
  (load-delivery-items!)
  (hook-browser-navigation!)
  (mount-components))
