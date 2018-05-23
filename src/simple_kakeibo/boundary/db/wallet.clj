(ns simple-kakeibo.boundary.db.wallet
  (:require [clojure.java.jdbc :as jdbc]
            [duct.database.sql]
            [clj-time.coerce :as c])
  (:refer-clojure :exclude [list update]))

(defprotocol Wallet
  (list [db])
  (create [db name])
  (update [db id charge])
  (delete [db id]))

(extend-protocol Wallet
  duct.database.sql.Boundary
  (list [{:keys [spec]}]
    (let [events (jdbc/query spec ["SELECT * FROM wallet LEFT OUTER JOIN wallet_event ON wallet.id = wallet_event.wallet_id"])]
      (->> events
           (group-by :id)
           (filter (fn [[k v]]
                     (->> v
                          (map #(= "DELETE" (:type %)))
                          (some true?)
                          not)))
           (map (fn [[k v]]
                  (let [charges (->> v
                                     (filter #(= "CHARGE" (:type %)))
                                     (map #(select-keys % [:event_date :charge]))
                                     (map #(assoc % :event_date_unix (c/to-long (:event_date %))))
                                     (map #(dissoc % :event_date)))]
                    {:id k :name (:name (first v))
                     :charges charges
                     :ammount (->> charges
                                   (map :charge)
                                   (reduce +))}))))))
  (create [{:keys [spec]} name]
    (jdbc/execute! spec ["INSERT INTO wallet VALUES (nextval('wallet_id'), ?)" name])
    (first (jdbc/query spec ["SELECT currval('wallet_id') as id"])))
  (update [{:keys [spec] :as db} id post-ammount]
    (let [id (Integer. id)
          post-ammount (Integer. post-ammount)
          pre-ammount (->> (list db)
                           (filter #(== id (:id %)))
                           first
                           :ammount)
          charge (- post-ammount pre-ammount)]
      (jdbc/execute! spec ["INSERT INTO wallet_event VALUES (nextval('wallet_event_id'), 'CHARGE', current_date, ?, ?)" charge id])))
  (delete [{:keys [spec]} id]
    (let [id (Integer. id)]
      (jdbc/execute! spec ["INSERT INTO wallet_event VALUES (nextval('wallet_event_id'), 'DELETE', current_date, 0, ?)" id]))))


(def wallets [{:id 1, :name "銀行", :charges [{:charge 1000, :event_date_unix 1525849200000} {:charge 9002, :event_date_unix 1525935600000}], :ammount 10002} {:id 2, :name "財布", :charges [{:charge 100, :event_date_unix 1525935600000}], :ammount 100}])
(def payments [{:id 5, :name "ああああああああ", :due_date_unix 1525158000000, :ammount 1000, :settled? true, :settle_date_unix 1525935600000} {:id 2, :name "aaa", :due_date_unix 1525935600000, :ammount 3, :settled? true, :settle_date_unix 1525849200000} {:id 6, :name "aaaa", :due_date_unix 1527145200000, :ammount 223, :settled? false, :settle_date_unix nil} {:id 7, :name "eee", :due_date_unix 1527231600000, :ammount 333, :settled? false, :settle_date_unix nil} {:id 4, :name "bbb", :due_date_unix 1527750000000, :ammount -200, :settled? true, :settle_date_unix 1525849200000}])
(let [current {:ammount (->> wallets
                             (map :ammount)
                             (reduce +))
               :date_unix (->> wallets
                               (map :charges)
                               flatten
                               (map :event_date_unix)
                               sort
                               last)}
      past-payments (->> payments
                         (filter #(>= (:date_unix current) (:due_date_unix %)))
                         (filter #(not (:settled? %))))
      post-payments (->> payments
                         (filter #(< (:date_unix current) (:due_date_unix %))))
      current (assoc current :ammount (apply + (:ammount current) (map :charge past-payments)))]
  (->> wallets
       (map :charges)
       flatten
       (sort-by :event_date_unix)
       reverse
       (reduce (fn [result data]
                 (let [pre-data (first result)]
                   (println pre-data data)
                   (cons {:ammount (- (:ammount pre-data) (:charge data))
                          :date_unix (:event_date_unix data)} result)))
               [current])))
