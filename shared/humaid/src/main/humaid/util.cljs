(ns humaid.util
  (:require [camel-snake-kebab.core :as csk]))

(defn map->nsmap
  ;; Adds namespace and transforms keys to kebab-case.
  ;; https://stackoverflow.com/a/43722784/1856086
  [m n]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (and (keyword? k)
                                     (not (qualified-keyword? k)))
                              (keyword (str n) (csk/->kebab-case k))
                              k)]
                 (assoc acc new-kw v)))
             {} m))

(defn str->int [s] (js/parseInt s))
