(ns humaid.app
  (:require
   [ajax.core :as ajax]
   [humaid.events]
   [humaid.router]
   [humaid.subs]
   [humaid.views]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   ))


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

;; (defn load-client! [id]
;;   (GET (str API-ADDR "/clients/" id "?fetch=diseases")
;;        {:response-format :json
;;         :keywords? true
;;         :handler #(swap! state assoc :client %)
;;         :error-handler
;;         #(if (= 404 (:status %))
;;            (do (ntfc/danger! (gstring/format "Клиент (id = %d) не найден" id))
;;                (set-hash! "/"))
;;            (ntfc/danger! "Ошибка при загрузке клиента. Попробуйте перезагрузить страницу."))
;;         }))

;; (defn load-client-deliveries! [client-id]
;;   (GET (str API-ADDR "/clients/" client-id "/deliveries")
;;        {:response-format :json
;;         :keywords? true
;;         :handler #(swap! state assoc :client-deliveries %)
;;         :error-handler
;;         #(if (= 404 (:status %))
;;            (do (ntfc/danger! (gstring/format "Клиент (id = %d) не найден" client-id))
;;                (set-hash! "/"))
;;            (ntfc/danger! "Ошибка при загрузке клиента. Попробуйте перезагрузить страницу."))
;;         }))

;; (defn load-client-services! [client-id]
;;   (GET (str API-ADDR "/clients/" client-id "/services")
;;        {:response-format :json
;;         :keywords? true
;;         :vec-strategy :rails ;; https://cljdoc.org/d/cljs-ajax/cljs-ajax/0.8.0/api/ajax.url
;;         :params {:types (keys delivery-item-categories)}
;;         :handler #(swap! state assoc :client-services %)
;;         :error-handler
;;         #(if (= 404 (:status %))
;;            (do (ntfc/danger! (gstring/format "Клиент (id = %d) не найден" client-id))
;;                (set-hash! "/"))
;;            (ntfc/danger! "Ошибка при загрузке клиента. Попробуйте перезагрузить страницу."))
;;         }))

;; (defn load-delivery-items! []
;;   (GET (str API-ADDR "/delivery_items")
;;        {:response-format :json
;;         :keywords? true
;;         :handler #(swap! state assoc :humaid-items %)
;;         :error-handler
;;         #(ntfc/danger! "Ошибка при получении списка вещей. Попробуйте перезагрузить страницу.")
;;         }))


;; -------------------------

(defn mount-components []
  (rdom/render [#'humaid.views/notification] (.getElementById js/document "notification"))
  (rdom/render [#'humaid.views/page] (.getElementById js/document "app")))

(defn init! []
  (swap! ajax/default-interceptors concat [auth-interceptor])
  (humaid.router/hook-browser-navigation!)
  (rf/dispatch-sync [:delivery-items-get])
  (mount-components))
