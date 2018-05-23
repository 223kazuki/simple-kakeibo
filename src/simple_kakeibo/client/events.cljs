(ns simple-kakeibo.client.events
  (:require [re-frame.core :as re-frame]
            [simple-kakeibo.client.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [ajax.protocols :as protocol]))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::set-active-panel
 (fn  [db [_ active-panel]]
   (-> db
       (assoc :active-panel active-panel))))

(re-frame/reg-event-db
 ::update-form
 (fn [db [_ name update-fn]]
   (update-in db [:form (keyword name)] update-fn)))

(re-frame/reg-event-fx
 ::fetch-wallets
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :http-xhrio {:method          :get
                 :uri             "/api/wallets"
                 :timeout         8000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::fetch-wallets-success]
                 :on-failure      [::api-failure]}}))

(re-frame/reg-event-db
 ::fetch-wallets-success
 (fn [db [_ result]]
   (assoc db
          :loading? false
          :wallets result)))

(re-frame/reg-event-fx
 ::create-wallet
 (fn [{:keys [db]} [_]]
   (let [{:keys [wallet/name]} (:form db)]
     {:db   (assoc db :loading? true)
      :http-xhrio {:method          :post
                   :uri             (str "/api/wallets")
                   :timeout         8000
                   :response-format (ajax/json-response-format {:keywords? true})
                   :params          {:name name}
                   :format          (ajax/json-request-format)
                   :on-success      [::create-wallet-success]
                   :on-failure      [::api-failure]}})))

(re-frame/reg-event-db
 ::create-wallet-success
 (fn [db [_ result]]
   (let [id (:id result)
         {:keys [wallet/ammount]} (:form db)]
     (re-frame/dispatch [::update-wallet id ammount]))
   (assoc db :loading? true)))

(re-frame/reg-event-fx
 ::update-wallet
 (fn [{:keys [db]} [_ id ammount]]
   {:db   (assoc db :loading? true)
    :http-xhrio {:method          :put
                 :uri             (str "/api/wallets/" id)
                 :timeout         8000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          {:ammount ammount}
                 :format          (ajax/json-request-format)
                 :on-success      [::fetch-wallets]
                 :on-failure      [::api-failure]}}))

(re-frame/reg-event-fx
 ::delete-wallet
 (fn [{:keys [db]} [_ id]]
   {:db   (assoc db :loading? true)
    :http-xhrio {:method          :delete
                 :uri             (str "/api/wallets/" id)
                 :timeout         8000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          nil
                 :format          (ajax/json-request-format)
                 :on-success      [::fetch-wallets]
                 :on-failure      [::api-failure]}}))

(re-frame/reg-event-fx
 ::fetch-payments
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :http-xhrio {:method          :get
                 :uri             "/api/payments"
                 :timeout         8000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::fetch-payments-success]
                 :on-failure      [::api-failure]}}))

(re-frame/reg-event-db
 ::fetch-payments-success
 (fn [db [_ result]]
   (assoc db
          :loading? false
          :payments result)))

(re-frame/reg-event-fx
 ::create-payment
 (fn [{:keys [db]} [_]]
   (let [{:keys [payment/name payment/due_date]} (:form db)]
     {:db   (assoc db :loading? true)
      :http-xhrio {:method          :post
                   :uri             (str "/api/payments")
                   :timeout         8000
                   :response-format (ajax/json-response-format {:keywords? true})
                   :params          {:name name :due_date_unix (.unix due_date)}
                   :format          (ajax/json-request-format)
                   :on-success      [::create-payment-success]
                   :on-failure      [::api-failure]}})))

(re-frame/reg-event-db
 ::create-payment-success
 (fn [db [_ result]]
   (let [id (:id result)
         {:keys [payment/ammount]} (:form db)]
     (re-frame/dispatch [::update-payment id ammount]))
   (assoc db :loading? true)))

(re-frame/reg-event-fx
 ::update-payment
 (fn [{:keys [db]} [_ id ammount]]
   {:db   (assoc db :loading? true)
    :http-xhrio {:method          :put
                 :uri             (str "/api/payments/" id)
                 :timeout         8000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          {:ammount ammount}
                 :format          (ajax/json-request-format)
                 :on-success      [::fetch-payments]
                 :on-failure      [::api-failure]}}))

(re-frame/reg-event-fx
 ::delete-payment
 (fn [{:keys [db]} [_ id]]
   {:db   (assoc db :loading? true)
    :http-xhrio {:method          :delete
                 :uri             (str "/api/payments/" id)
                 :timeout         8000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          nil
                 :format          (ajax/json-request-format)
                 :on-success      [::fetch-payments]
                 :on-failure      [::api-failure]}}))

(re-frame/reg-event-fx
 ::settle-payment
 (fn [{:keys [db]} [_ id]]
   {:db   (assoc db :loading? true)
    :http-xhrio {:method          :post
                 :uri             (str "/api/payments/" id "/settle")
                 :timeout         8000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          nil
                 :format          (ajax/json-request-format)
                 :on-success      [::fetch-payments]
                 :on-failure      [::api-failure]}}))

(re-frame/reg-event-fx
 ::unsettle-payment
 (fn [{:keys [db]} [_ id]]
   {:db   (assoc db :loading? true)
    :http-xhrio {:method          :post
                 :uri             (str "/api/payments/" id "/unsettle")
                 :timeout         8000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params          nil
                 :format          (ajax/json-request-format)
                 :on-success      [::fetch-payments]
                 :on-failure      [::api-failure]}}))

(re-frame/reg-event-db
 ::api-failure
 (fn [db [_ result]]
   (if (== (:status result) 401)
     (aset js/window.location "href" "/login")
     (assoc db :loading? false))))
