(ns humaid.app-test
  (:require
   [cljs.test :refer (deftest is)]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen]
   [clojure.test.check.generators]
   [humaid.app :as a]
   [humaid.date :as d]
   ))

(deftest delivery-unavailable-until-test
  (let [deliveries [{::a/delivery-item-id 10 ::a/delivered-at (d/str->date "2020-08-17")}]
        delivery-item {::a/id 5 ::a/limit-days 5}
        today (d/today)]
    (is (= nil
           (a/delivery-unavailable-until deliveries delivery-item today)
           ))
    ))

(deftest delivery-unavailable-until-2-test
  (let [deliveries [{::a/delivery-item-id 10 ::a/delivered-at (d/str->date "2020-08-17")}]
        delivery-item {::a/id 10 ::a/limit-days 3}
        today (d/str->date "2020-08-18")]
    (is (= (d/str->date "2020-08-20")
           (a/delivery-unavailable-until deliveries delivery-item today)))))
