(ns humaid.ajax
  (:require
   [goog.object :as gobject]
   [ajax.interceptors :refer
    [map->ResponseFormat]]
   [ajax.protocols :refer
    [-body]]
   [camel-snake-kebab.core :as csk]))

(defn js->clj2
  "Patched copy of js->clj funciton from core.cljs which allows passing of `:key-fn` option"
  ([x] (js->clj x :keywordize-keys false))
  ([x & opts]
    (let [{:keys [keywordize-keys key-fn transform-map]} opts
          keyfn (cond
                  key-fn key-fn
                  keywordize-keys keyword
                  :else str)
          f (fn thisfn [x]
              (cond
                (satisfies? IEncodeClojure x)
                (-js->clj x (apply array-map opts))

                (seq? x)
                (doall (map thisfn x))

                (map-entry? x)
                (MapEntry. (thisfn (key x)) (thisfn (val x)) nil)

                (coll? x)
                (into (empty x) (map thisfn) x)

                (array? x)
                (persistent!
                 (reduce #(conj! %1 (thisfn %2))
                         (transient []) x))

                (identical? (type x) js/Object)
                (persistent!
                 (reduce (fn [r js-key]
                           (let [k (keyfn js-key)
                                 transform-fn (or (get transform-map k) thisfn)]
                             (assoc! r k (transform-fn (gobject/get x js-key)))))
                           (transient {}) (js-keys x)))
                :else x))]
      (f x))))

(defn key-fn-ns [ns]
  {:key-fn #(keyword ns (csk/->kebab-case %))})

(defn json-resp [opts]
  (let [{:keys [key-fn transform-map]} opts]
    (map->ResponseFormat
     {:read (fn json-read-response-format [xhrio]
               (js->clj2 (.parse js/JSON (-body xhrio))
                         :key-fn key-fn
                         :transform-map transform-map))
       :description "JSON"
       :content-type ["application/json"]})))
