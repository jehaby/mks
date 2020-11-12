(ns humaid.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :active-page
 (fn [db _]
   (:active-page db)))

(reg-sub
 :page-params
 (fn [db _]
   (:page-params db)))

(reg-sub
 :search
 (fn [db _] (:search db)))

(reg-sub
 :loading
 (fn [db _] (:loading db)))

(reg-sub
 :client
 (fn [db _] (:client db)))

(reg-sub
 :client-deliveries
 (fn [db _] (:client-deliveries db)))

(reg-sub
 :client-services
 (fn [db _] (:client-services db)))

(reg-sub
 :delivery-items
 (fn [db _] (:delivery-items db)))

(reg-sub
 :notification
 (fn [db _] (:notification db)))
