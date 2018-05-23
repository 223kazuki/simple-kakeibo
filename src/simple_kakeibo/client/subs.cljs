(ns simple-kakeibo.client.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::active-panel
 (fn [db]
   (:active-panel db)))

(re-frame/reg-sub
 ::loading?
 (fn [db]
   (:loading? db)))

(re-frame/reg-sub
 ::form
 (fn [db]
   (:form db)))

(re-frame/reg-sub
 ::wallets
 (fn [db]
   (:wallets db)))

(re-frame/reg-sub
 ::payments
 (fn [db]
   (:payments db)))
