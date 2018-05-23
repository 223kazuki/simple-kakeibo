(ns simple-kakeibo.handler.index
  (:require [ataraxy.core :as ataraxy]
            [ataraxy.response :as response]
            [clojure.java.io :as io]
            [integrant.core :as ig]))

(defmethod ig/init-key :simple-kakeibo.handler/login [_ options]
  (fn [{[_] :ataraxy/result}]
    [::response/ok (slurp (io/resource "simple_kakeibo/public/index.html"))]))

(defmethod ig/init-key :simple-kakeibo.handler/logout [_ options]
  (fn [{[_] :ataraxy/result}]
    {:status  302
     :headers {"Location" "/login"}
     :session nil
     :body    ""}))

(defmethod ig/init-key :simple-kakeibo.handler/index [_ options]
  (fn [{[_] :ataraxy/result}]
    [::response/ok (slurp (io/resource "simple_kakeibo/public/index.html"))]))
