(ns simple-kakeibo.boundary.db.payment
  (:require [clojure.java.jdbc :as jdbc]
            [duct.database.sql]
            [clj-time.coerce :as c]
            [clj-time.jdbc])
  (:refer-clojure :exclude [list update]))

(defprotocol payment
  (list [db])
  (create [db name due_date_unix])
  (update [db id charge])
  (settle [db id])
  (unsettle [db id])
  (delete [db id]))

(extend-protocol payment
  duct.database.sql.Boundary
  (list [{:keys [spec]}]
    (let [events (jdbc/query spec ["SELECT * FROM payment LEFT OUTER JOIN payment_event ON payment.id = payment_event.payment_id"])]
      (->> events
           (group-by :id)
           (filter (fn [[k v]]
                     (->> v
                          (map #(= "DELETE" (:type %)))
                          (some true?)
                          not)))
           (map (fn [[k v]]
                  (let [settled? (pos?
                                  (- (count (filter #(= "SETTLE" (:type %)) v))
                                     (count (filter #(= "UNSETTLE" (:type %)) v))))]
                    {:id k :name (:name (first v)) :due_date_unix (c/to-long (:due_date (first v)))
                     :ammount (->> v
                                   (filter #(= "CHARGE" (:type %)))
                                   (map :charge)
                                   (reduce +))
                     :settled? settled?
                     :settle_date_unix (when settled?
                                         (->> v
                                              (filter #(= "SETTLE" (:type %)))
                                              (map :event_date)
                                              sort
                                              last
                                              c/to-long))}))))))
  (create [{:keys [spec]} name due_date_unix]
    (let [due_date (c/from-long (* due_date_unix 1000))]
      (jdbc/execute! spec ["INSERT INTO payment VALUES (nextval('payment_id'), ?, ?)" name due_date]))
    (first (jdbc/query spec ["SELECT currval('payment_id') as id"])))
  (update [{:keys [spec] :as db} id post-ammount]
    (let [id (Integer. id)
          post-ammount (Integer. post-ammount)
          pre-ammount (->> (list db)
                           (filter #(== id (:id %)))
                           first
                           :ammount)
          charge (- post-ammount pre-ammount)]
      (jdbc/execute! spec ["INSERT INTO payment_event VALUES (nextval('payment_event_id'), 'CHARGE', current_date, ?, ?)" charge id])))
  (settle [{:keys [spec]} id]
    (let [id (Integer. id)]
      (jdbc/execute! spec ["INSERT INTO payment_event VALUES (nextval('payment_event_id'), 'SETTLE', current_date, 0, ?)" id])))
  (unsettle [{:keys [spec]} id]
    (let [id (Integer. id)]
      (jdbc/execute! spec ["INSERT INTO payment_event VALUES (nextval('payment_event_id'), 'UNSETTLE', current_date, 0, ?)" id])) )
  (delete [{:keys [spec]} id]
    (let [id (Integer. id)]
      (jdbc/execute! spec ["INSERT INTO payment_event VALUES (nextval('payment_event_id'), 'DELETE', current_date, 0, ?)" id]))))
