(ns humaid.views
  (:require
   [clojure.string :as string]
   [goog.string :as gstring]
   [goog.string.format]
   [humaid.date :as date]
   [humaid.config :refer [APP-PREFIX MKS-ADDR]]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent-keybindings.keyboard :as kb]
   ))

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

(defn client-fullname [client]
  (str (:lastname client) " " (:firstname client) " " (:middlename client)))

(defn client-photo [photo-name]
  ;; TODO: default photo
  (when photo-name
    (str MKS-ADDR "/uploads/images/client/photo/" (subs photo-name 0 2) "/" photo-name)))

(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (rfe/href k params query)))


(defn stopwatch-ui []
  (let [v (:val @stopwatch)
        min  (quot v 60)
        sec  (rem v 60)]
    [:div
     {:class ["is-size-2" (when (> min 4) "has-text-warning")]}
     (gstring/format "%02d:%02d" min sec)]))

(defn notification []
  (let [s @(subscribe [:notification])]
    (when-not (empty? s)
      [:div {:class ["notification" (str "is-" (name (:kind s)))]}
       [:button.delete {:on-click #(dispatch [:clear-notification])}]
       (:msg s)])))

(defn modal-loading []
  [:div.modal.is-active
   [:div.modal-backgroud]
   [:div.modal-content
    [:div.content "Загружается..."]
    [:button.button.is-large.is-loading " -----------"]]])

(defn start-page []
  (r/with-let [_  (reset-stopwatch!)]
    [:section.section>div.container>div.content
     [:section.columns
      [:div.column.is-2
       [stopwatch-ui]]

      (let [clients @(subscribe [:search])
            loading @(subscribe [:loading])
            not-found (= clients [])]

        [:div.column
         [:h3 "Поиск клиента"]
         [:div {:class ["control" (when (:search loading) "is-loading")]}
          [:input
           {:class ["input" "is-large" (when not-found "is-danger")]
            :type "text"
            :placeholder "ФИО..."
            :on-change #(dispatch [:client-search (-> % .-target .-value)])
            }]
          (when not-found [:p.help.is-danger "Не нашли =("])]

         (for [{:keys [id birthDate] :as client} clients
               :let [birth-date (date/date->yy-mm-dd (js/Date. birthDate))]]
           ^{:key id}
           [:div.is-size-2
            [:a {:href (href :client {:client-id id})}
             (str (client-fullname client) " (" birth-date ")")]]
           )])]]))

(defn delivery-link [id kind]
  (href :delivery {:client-id id :delivery-items-kind kind}))

(defn client-page []

  ;;  TODO: stopwatch

  (let [loading @(subscribe [:loading])
        client @(subscribe [:client])]
    (if (:client loading)

      [modal-loading]

      [:section.section.container.content
       [:section.columns
        [:div.column.is-2
         [stopwatch-ui]
         [:button.button {:on-click #(dispatch [:push-state :start])} "Завершить выдачу"]]

        (let [clothes (delivery-link (:id client) :clothes)
              hygiene (delivery-link (:id client) :hygiene)
              crutches (delivery-link (:id client) :crutches)]
          [:div.column.is-3
           [:p>a {:href clothes} [:button.button.is-large.is-block "1. Одежда"]]
           [:p>a {:href hygiene} [:button.button.is-large.is-block "2. Гигиена"]]
           [:p>a {:href crutches}[:button.button.is-large.is-block "3. Костыли/трости"]]

           ;; [kb/kb-action "1" #(set-hash! clothes)]
           ;; [kb/kb-action "2" #(set-hash! hygiene)]
           ;; [kb/kb-action "3" #(set-hash! crutches)]
           ])

        [:div.column.is-3
         [:img {:src (client-photo (:photo_name client))}]
         [:p.is-size-5 (client-fullname client)]
         (if (some #{"Туберкулез"} (:diseases client))
           [:p.is-size-5.has-text-danger "Болеет туберкулёзом!"])]]]
      )))

(defn redirect-modal [state]
  [:div.modal {:class [(when (:active? @state) "is-active")]}
   [:div.modal-background]
   [:div.modal-card
    [:div.modal-card-head]
    [:div.modal-card-body
     [:div.content "Есть несохранённые изменения. Продолжить?"]]
    [:div.modal-card-foot
     [:button.button {:on-click (:redirect-fn @state)} "Да"]
     [:button.button {:on-click #(swap! state assoc :active? false) :aria-label "close"} "Нет"]]]])

(defn delivery-page []
  (r/with-let [
               ;; _ (start-stopwatch!)
               ;; _ (when-not (:client @state)
               ;;     (do (load-client! id)
               ;;         (load-client-deliveries! id)
               ;;         (load-client-services! id)))

               hotkeys "1234567890qwertyuiopasdfghjklzxcvbnm[];',./"
               kind (keyword (:delivery-items-kind @(subscribe [:path-params])))

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

               redirect-modal-state (r/atom {:active? false})
               redirect! (fn [selected-items page & params]
                           (let [redirect-fn #(dispatch (-> (concat [:push-state page] params) vec))]
                             (if (empty? @selected-items)
                               (redirect-fn)
                               (swap! redirect-modal-state assoc
                                      :active? true
                                      :redirect-fn redirect-fn))))

               delivery-unavailable-until
               (fn
                 ;; deliveries -- list of items delivered to a client (list of maps with :deliveryItemID and :deliveredAt keys).
                 ;; When current item (2nd arg) cannot be issued today returns nearest available date.
                 [deliveries {item-id :id limit-days :limitDays}]
                 (when-let [item-delivery (some
                                           #(when (= (str item-id) (:deliveryItemID %)) %)
                                           deliveries)]
                   (let [date (js/Date. (:deliveredAt item-delivery))
                         next-available-date (date/add-days  date limit-days)]
                     (when (< (date/today) next-available-date)
                       next-available-date))))]

    (let [loading @(subscribe [:loading])
          client @(subscribe [:client])
          client-deliveries @(subscribe [:client-deliveries])
          delivery-items @(subscribe [:delivery-items])]

      (if (:client loading)
        [modal-loading]

        [:section.section.container.content

         [redirect-modal redirect-modal-state]

         [:section.columns
          [:div.column.is-2
           [stopwatch-ui]
           [:p>button.button
            {:on-click #(redirect! selected-items :client {:client-id (:id client)})}
            "Вернуться к списку"]
           [:p>button.button.is-light.is-danger
            {:on-click #(redirect! selected-items :start)}
            "Завершить выдачу"]]

          [:section.column
           (let [selected @selected-items
                 items (filter #(= category-id (:category %)) delivery-items)]
             [:div.container.content
              [:h3 heading]
              [:p.is-size-5.has-text-weight-light (str " " (:name client))]
              [:section

               (for [[{:keys [id name category limitDays] :as item} key] (map vector items (concat hotkeys (repeat nil)))
                     :let [unavailable-until (delivery-unavailable-until client-deliveries item)
                           selected? (contains? selected id)]]
                 [:<> {:key id}
                  [:button
                   {:class ["button" "mx-2" "my-2" "disabled"
                            (when selected? "is-success")
                            (when (and unavailable-until (not selected?)) "is-light")]
                    :on-click #(switch-selected! selected-items id)}

                   (str (when key (str key ". ")) (string/capitalize name))
                   (when unavailable-until
                     [:span.has-text-weight-light.is-size-6.mx-1
                      (str "(доступно с " (date/date->mm-dd unavailable-until) ")")])]
                  (when key
                    [kb/kb-action key #(switch-selected! selected-items id)])])]])

           (when-let [services
                      (seq
                       (filter
                        #(= (str category-id) (:type %))
                        @(subscribe [:client-services])))]
             [:div.container.content
              [:h5 "Выдачи-услуги"]
              [:ul
               (for [{comment :comment created-at :createdAt} services
                     :let [created-at (date/date->yy-mm-dd (js/Date. created-at))]]
                 [:li (str comment " (" created-at ")")])]])

           [:div.container.content
            [:button.button.is-light.is-success
             {:disabled (empty? @selected-items)
              :on-click #(dispatch [:save-deliveries (:id client) @selected-items])}
             "Сохранить"]
;;            [kb/kb-action "ctrl-enter" #(save-items! (:id client) selected-items)]
            ]]]]
        ))))

(defn not-found-page []
  [:section.section>div.container>div.content
   [:p "404 (страница не найдена)"]
   [:p  [:a {:href (href :start)} "назад"]]])

(defn page [{:keys [router]}]
  (let [current-route @(subscribe [:current-route])]
    (if current-route
      [(-> current-route :data :view)]
      [not-found-page])))
