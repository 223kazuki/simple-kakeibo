(ns simple-kakeibo.handler.api
  (:require [ataraxy.core :as ataraxy]
            [ataraxy.response :as response]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [clojure.string :as str]
            [ring.util.response :refer [file-response]]
            [simple-kakeibo.boundary.db.payment :as payment]
            [simple-kakeibo.boundary.db.wallet :as wallet]
            [clj-time.coerce :as c]
            [clj-time.local :as l]))

(defmethod ig/init-key ::login [_ {:keys [login-email login-password]}]
  (when-not (and login-email login-password)
    (throw (Exception. (str "Initialization error."
                            "You have to set LOGIN_EMAIL and LOGIN_PASSWORD as environment variables."))))
  (fn [{[_ params] :ataraxy/result}]
    (let [{:keys [email password]} params]
      (if (and (= email login-email)
               (= password login-password))
        {:status 200
         :session {:identity email}
         :body {:message "Login suceed."}}
        {:status 401
         :body {:message "Login failed."}}))))

(defmethod ig/init-key ::payments [_ {:keys [db]}]
  (fn [{[_] :ataraxy/result}]
    [::response/ok (payment/list db)]))

(defmethod ig/init-key ::wallets [_ {:keys [db]}]
  (fn [{[_] :ataraxy/result}]
    [::response/ok (wallet/list db)]))

(defmethod ig/init-key :simple-kakeibo.handler.api.payments/create [_ {:keys [db]}]
  (fn [{[_ {:keys [name due_date_unix]}] :ataraxy/result}]
    (println due_date_unix)
    (if (and name due_date_unix)
      (let [r (payment/create db name due_date_unix)]
        {:status 201
         :body r})
      [::response/forbidden])))

(defmethod ig/init-key :simple-kakeibo.handler.api.wallets/create [_ {:keys [db]}]
  (fn [{[_ params] :ataraxy/result}]
    (if-let [name (:name params)]
      (let [r (wallet/create db name)]
        {:status 201
         :body r})
      [::response/forbidden])))

(defmethod ig/init-key :simple-kakeibo.handler.api.payments/update [_ {:keys [db]}]
  (fn [{[_ id params] :ataraxy/result}]
    (if-let [ammount (:ammount params)]
      (let [r (payment/update db id ammount)]
        {:status 201 :body {:id id}})
      [::response/forbidden])))

(defmethod ig/init-key :simple-kakeibo.handler.api.payments/settle [_ {:keys [db]}]
  (fn [{[_ id] :ataraxy/result}]
    (payment/settle db id)
    [::response/ok {}]))

(defmethod ig/init-key :simple-kakeibo.handler.api.payments/unsettle [_ {:keys [db]}]
  (fn [{[_ id] :ataraxy/result}]
    (payment/unsettle db id)
    [::response/ok {}]))

(defmethod ig/init-key :simple-kakeibo.handler.api.wallets/update [_ {:keys [db]}]
  (fn [{[_ id params] :ataraxy/result}]
    (if-let [ammount (:ammount params)]
      (let [r (wallet/update db id ammount)]
        {:status 201 :body {:id id}})
      [::response/forbidden])))

(defmethod ig/init-key :simple-kakeibo.handler.api.payments/delete [_ {:keys [db]}]
  (fn [{[_ id] :ataraxy/result}]
    (payment/delete db id)
    [::response/ok {}]))

(defmethod ig/init-key :simple-kakeibo.handler.api.wallets/delete [_ {:keys [db]}]
  (fn [{[_ id] :ataraxy/result}]
    (wallet/delete db id)
    [::response/ok {}]))
