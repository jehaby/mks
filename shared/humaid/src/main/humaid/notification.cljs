(ns humaid.notification
  (:require
   [reagent.core :as r]))

(def state (r/atom {}))

(defn clear! []
  (reset! state {}))

(defn show!
  "kind - bulma class (primary, link, info, success, warning, danger)  https://bulma.io/documentation/elements/notification/"
  [kind msg]
  (do (reset! state {:kind kind :msg msg})
      ;; TODO: bug (bad behavior): 1) show! {:msg "n1"}, ... after 4.1999 sec 2) show! {:msg "n2"} -> user doesn't see n2
      (js/setTimeout clear! 4200)))

(defn success! [msg] (show! "success" msg))
(defn danger! [msg] (show! "danger" msg))

(defn notification []
  (let [s @state]
    (when-not (empty? s)
      [:div {:class ["notification" (str "is-" (name (:kind s)))]}
       [:button.delete {:on-click clear!}]
       (:msg s)])))
