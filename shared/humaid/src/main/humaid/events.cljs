(ns humaid.events
  (:require
   [ajax.core :refer [json-request-format json-response-format]]
   [day8.re-frame.http-fx]
   [clojure.string :as string]
   [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch]]
   [humaid.config :refer [API-ADDR]]))


;; Notification
;; --------------------------------------

(defonce timeouts (reagent.core/atom {}))

(reg-fx
 :timeout
 (fn [{:keys [id event time]}]
   (when-some [existing (get @timeouts id)]
     (js/clearTimeout existing)
     (swap! timeouts dissoc id))
   (when (some? event)
     (swap! timeouts assoc id
            (js/setTimeout
             (fn []
               (dispatch event))
             time)))))

(reg-event-fx
 :show-notification
 (fn [{:keys [db]} [_ kind msg]]
   (prn "IN SHOW NOT HNDL" kind msg)
   {:db (assoc db :notification {:kind kind :msg msg})
    :timeout {:id :notification
              :event [:clear-notification]
              :time 4200}}))

(reg-event-db
 :clear-notification
 (fn [db]
   (dissoc db :notification)))


;; API calls
;; --------------------------------------

(defn set-loading [db key val]
  (assoc-in db [:loading key] val))

(reg-event-fx
 :client-search
 (fn [{:keys [db]} [_ search-str]]
   (let [search-str (string/trim search-str)]
     (if (> (count search-str) 2)
       {:http-xhrio {:method :get
                     :uri (str API-ADDR "/clients/search")
                     :params {:v search-str}
                     :response-format (json-response-format {:keywords? true})
                     :on-success [:api-request-success :search]
                     :on-failure [:api-request-error :search]}
        :db (set-loading db :search true)
        ;; TODO: start stopwatch
        }
       {:db (dissoc db :search)}))))

(reg-event-fx
 :client-get
 (fn [{:keys [db]} [_ client-id]]
   (prn "IN CLIENT_GET " client-id)
   (when (or (empty? (:client db))
             (not= (str (get-in db [:client :id])) client-id))
     {:db (assoc-in db [:loading :client] true)
      :http-xhrio {:method :get
                   :uri (str API-ADDR "/clients/" client-id)
                   :params {:fetch "diseases"}
                   :response-format (json-response-format {:keywords? true})
                   :on-success [:api-request-success :client]
                   :on-failure [:client-get-failure]}
      })))

(reg-event-fx
 :client-get-failure
 (fn [{:keys [db]} [_ resp]]
   {:db (-> db
            (set-loading :client false)
            (assoc-in [:search] []))

    :dispatch-n [[:set-active-page {:page :start}]
                 (if (= 404 (:status resp))
                   [:show-notification :danger "Клиент не найден"]
                   [:api-request-error :client resp])]}))

(reg-event-fx
 :client-get-deliveries
 (fn [{:keys [db]} [_ client-id]]
   {:db (assoc-in db [:loading :client-deliveries] true)
    :http-xhrio {:method :get
                 :uri (str API-ADDR "/clients/" client-id "/deliveries")
                 :response-format (json-response-format {:keywords? true})
                 :on-success [:api-request-success :client-deliveries]
                 :on-failure [:api-request-error :client-deliveries]}}))

(reg-event-fx
 :delivery-items-get
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:loading :delivery-items] true)
    :http-xhrio {:method :get
                 :uri (str API-ADDR "/delivery_items")
                 :response-format (json-response-format {:keywords? true})
                 :on-success [:api-request-success :delivery-items]
                 :on-failure [:api-request-error :delivery-items]}}))

(def delivery-item-categories {3 "Одежда"
                               17 "Гигиена"
                               22 "Костыли/трости"})

(reg-event-fx
 :client-get-services
 (fn [{:keys [db]} [_ client-id]]
   {:db (assoc-in db [:loading :client-services] true)
    :http-xhrio {:method :get
                 :uri (str API-ADDR "/clients/" client-id "/services")
                 :params {:types (keys delivery-item-categories)}
                 :vec-strategy :rails
                 :response-format (json-response-format {:keywords? true})
                 :on-success [:api-request-success :client-services]
                 :on-failure [:api-request-error :client-services]}}))

(reg-event-fx
 :save-deliveries
 (fn [{:keys [db]} [_ client-id item-ids]]
   {:db (set-loading db [:save-deliveries] true)
    :http-xhrio {:method :post
                 :uri (str API-ADDR "/clients/" client-id "/deliveries")
                 :format (json-request-format {:keywords? true})
                 :response-format (json-response-format {:keywords? true})
                 :params {"item_ids" item-ids}
                 :on-success [:save-deliveries-success client-id]
                 :on-failure [:api-request-error :save-deliveries]}}))

(reg-event-fx
 :save-deliveries-success
 (fn [{:keys [db]} [_ client-id resp]]
   {:db (-> db
            (set-loading :save-deliveries false))
    :dispatch-n [[:show-notification :success "Выдача сохранена!"]
                 [:set-active-page {:page :client
                                    :params {:path {:client-id client-id}}}]]}))

(reg-event-db
 :api-request-success
 (fn [db [_ request-name resp]]
   (prn "IN  :api-request-success " request-name resp)
   (-> db
       (set-loading request-name false)
       (assoc request-name resp))))

(reg-event-fx
 :api-request-error
 (fn [db [_ request-type response]]
   (prn "Error in HTTP request. Response: " response)
   {:db (-> db
            (assoc-in [:errors request-type] (get-in response [:response :errors]))
            (set-loading request-type false))

    :dispatch [:show-notification :danger
               (str "Ошибка при запросе к серверу: " request-type)]}))

(reg-event-fx
 :set-active-page
 (fn [{:keys [db]} [_ {:keys [page params]}]]

   ;; TODO: change browser url (effect!)

   (prn "in set active page: " page params)
   (let [set-page #(assoc % :active-page page)
         client-id (get-in params [:path :client-id])]
     (case page
       :start {:db (set-page db)}

       :client {:db (set-page db)
                :dispatch [:client-get client-id]}

       :delivery {:db (-> db
                          set-page
                          (assoc-in [:page-params :delivery-items-kind] (get-in params [:path :delivery-items-kind])))
                  :dispatch-n [[:client-get client-id]
                               [:client-get-deliveries client-id]
                               [:client-get-services client-id]]}))))
