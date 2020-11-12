(ns humaid.router
  (:require
   [clojure.string :as string]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [re-frame.core :as rf]
   [reitit.frontend :as reitit]

   )
  (:import goog.History)
  )

(def router
  (reitit/router
   [["/" :start]
    ["/clients/:client-id" :client]
    ["/clients/:client-id/delivery/:delivery-items-kind" :delivery]]))

(defn match-route [uri]
  (->> (or (not-empty (string/replace uri #"^.*#" "")) "/")
       (reitit/match-by-path router)))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     HistoryEventType/NAVIGATE
     (fn [event]
       (prn "EVENT IN HISTORY: " event)
       (prn "EVENT IN HISTORY 2: " (.-token event))
       (prn "EVENT IN HISTORY 3: " (match-route (.-token event)))
       (let [m (match-route (.-token event))]
         (rf/dispatch-sync [:set-active-page
                            {:page (get-in m [:data :name])
                             :params (:parameters m)}]))))
    (.setEnabled true)))

