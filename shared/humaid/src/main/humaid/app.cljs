(ns humaid.app
  (:require
   [ajax.core :refer [GET POST]]
   [clojure.string :as string]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [goog.string :as gstring]
   [goog.string.format]
   [humaid.date :as date]
   [humaid.notification :as ntfc]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [reitit.frontend :as rf])
  (:import goog.History))

(def config {:api-addr "/api/v1"
             :mks-addr ""
             :app-prefix "/humaid"
             :timer-duration-sec 120})

(def humaid-item-categories {3 "Одежда"
                             17 "Гигиена"
                             51 "Другое"})

;; State
;; --------------------

(defonce state (r/atom {:page :start}))

(defonce timer (r/atom {:val (:timer-duration-sec config)
                        :started false}))

;; Misc
;; --------------------

(defonce timer-fn (atom nil))
(defn reset-timer! []
  (js/clearInterval @timer-fn)
  (swap! timer assoc :val (:timer-duration-sec config)
         :started false))

(defn start-timer! []
  (when-not (:started @timer)
    (swap! timer assoc :started true)
    (reset! timer-fn
            (js/setInterval
             (fn [] (swap! timer update :val #(if (< 0 %) (dec %) %)))
             1000))))

(defn set-hash! [loc]
  (set! (.-hash js/window.location) loc))

(defn client-fullname [client]
  (str (:lastname client) " " (:firstname client) " " (:middlename client)))

(defn client-photo [photo-name]
  (str (:mks-addr config) "/uploads/images/client/photo/" (subs photo-name 0 2) "/" photo-name))


;; Requests
;; --------------------

(defn load-client! [id]
  (GET (str (:api-addr config) "/clients/" id)
       {:response-format :json
        :keywords? true
        :params {:humaid_deliveries 1
                 :services 1}
        :handler #(swap! state assoc :client %)
        :error-handler
        #(if (= 404 (:status %))
           (do (ntfc/danger! (gstring/format "Клиент (id = %d) не найден" id))
               (set-hash! "/"))
           (ntfc/danger! "Ошибка при загрузке клиента. Попробуйте перезагрузить страницу."))
        }))

(defn load-humaid-items! []
  (GET (str (:api-addr config) "/humaid_items")
       {:response-format :json
        :keywords? true
        :handler #(swap! state assoc :humaid-items %)
        :error-handler
        #(ntfc/danger! "Ошибка при получении списка вещей. Попробуйте перезагрузить страницу.")
        }))

(defn save-items! [client-id item-ids]
  (POST (str (:api-addr config) "/clients/" client-id "/humaiditem_delivery")
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
      (do (reset-timer!)
          (reset! search-res nil))
      (when (> (count name) 2)
        (do (start-timer!)
            (GET (str (:api-addr config) "/clients/search")
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

(defn timer-ui []
  (let [v (:val @timer)
        min  (quot v 60)
        sec  (rem v 60)]
    [:div.is-size-2 (gstring/format "%02d:%02d" min sec)]))

(defn modal-loading []
  [:div.modal.is-active
   [:div.modal-backgroud]
   [:div.modal-content
    [:div.content "Загружается..."]
    [:button.button.is-large.is-loading " -----------"]]])

(defn start-page []
  (r/with-let [search-res (r/atom nil)
               _  (reset-timer!)]
    [:section.section>div.container>div.content
     [:section.columns
      [:div.column.is-2
       [timer-ui]]

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
            [:a {:href (str (:app-prefix config) "/#/clients/" id)}
             (str (client-fullname client) " (" birth-date ")")]] ;; TODO styling
           )])]]))

(defn delivery-link [id kind]
  (str (:app-prefix config) "/#/clients/" id "/delivery/" (name kind)))

(defn client-page [{{id :id} :path}]
  (r/with-let [_ (load-client! id)
               _  (start-timer!)]
    (if-let [client (:client @state)]
      [:section.section.container.content
       [:section.columns
        [:div.column.is-2
         [timer-ui]
         [:button.button {:on-click #(set-hash! "/")} "Завершить выдачу"]]

        [:div.column.is-3
         [:p>a {:href (delivery-link (:id client) :clothes)}
          [:button.button.is-large.is-block "Одежда"]]
         [:p>a {:href (delivery-link (:id client) :hygiene)}
          [:button.button.is-large.is-block "Гигиена"]]
         [:p>a {:href (delivery-link (:id client) :crutches)}
          [:button.button.is-large.is-block "Костыли и трости"]]]

        [:div.column.is-3
         [:img
          {:src (client-photo (:photoName client))}]
         [:p.is-size-5 (client-fullname client)]
         ]]
       ]

      [modal-loading]
      )))

(defn redirect-modal [state]
  [:div.modal {:class [(when (:active? @state) "is-active")]} ;; TODO modal formatting
   [:div.modal-background]
   [:div.modal-card
    [:div.modal-card-head]
    [:div.modal-card-body
     [:div.content "Есть несохранённые изменения. Продолжить?"]]
    [:div.modal-card-foot
     [:button.button {:on-click #(set-hash! (:url @state))} "Да"]
     [:button.button {:on-click #(swap! state assoc :active? false) :aria-label "close"} "Нет"]]
    ]])

(defn delivery-page [{{kind :kind id :id} :path}]  ;; TODO: what if client-id and state client differs?
  (r/with-let [_ (start-timer!)
               _ (when-not (:client @state) (load-client! id))
               kind (keyword kind)
               category-id (kind {:clothes 3
                                  :hygiene 17
                                  :crutches 51})
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
                                    :url url)))

               delivery-unavailable-until
               (fn
                 ;; deliveries -- list of items delivered to a client (list of maps with :humAidItemID and :deliveredAt keys).
                 ;; In case current item (2nd arg) cannot be issued today returns nearest available date.
                 [deliveries {item-id :id limit-days :limitDays}]
                 (when-let [item-delivery (some
                                           #(when (= (str item-id) (:humAidItemID %)) %)
                                           deliveries)]
                   (let [date (js/Date. (:deliveredAt item-delivery))
                         next-available-date (date/add-days  date limit-days)]
                     (when (< (date/today) next-available-date)
                       next-available-date))))]

    (if-let [client (:client @state)]

      [:section.section.container.content

       [redirect-modal redirect-modal-state]

       [:section.columns
        [:div.column.is-2
         [timer-ui]
         [:p>button.button
          {:on-click #(redirect! (str "/clients/" id) selected-items)}
          "Вернуться к списку"]
         [:p>button.button.is-light.is-danger
          {:on-click #(redirect! "/" selected-items)}
          "Завершить выдачу"]]

        [:section.column
         (let [selected @selected-items
               deliveries (:humAidItems client)]
           [:div.container.content
            [:h3 heading]
            [:p.is-size-5.has-text-weight-light (str " " (:name client))]
            [:section

             (for [{:keys [id name category limitDays] :as item} (:humaid-items @state)
                   :when (= category category-id)
                   :let [unavailable-until (delivery-unavailable-until deliveries item)]]
               [:button
                {:key id
                 :class ["button" "is-large" "mx-2" "my-2" "disabled"
                         (when (contains? selected id) "is-success")]
                 :on-click #(switch-selected! selected-items id)
                 :dangerouslySetInnerHTML
                 {:__html (str
                           (clojure.string/capitalize name)
                           (when unavailable-until
                             (str "<br> (доступно с " (date/date->mm-dd unavailable-until) ")"))
                           )}}])
             ]])
         [:div.container.content
          [:button.button.is-large.is-light.is-success
           {:disabled (empty? @selected-items)
            :on-click #(save-items! (:id client) selected-items)}
           "Сохранить"]]]]]

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
    [(pages (:page s) #'not-found-page) (:parameters s)]))

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
  (load-humaid-items!)
  (hook-browser-navigation!)
  (mount-components))

