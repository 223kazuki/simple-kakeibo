(ns simple-kakeibo.client
  (:require [simple-kakeibo.client.login :as login]
            [simple-kakeibo.client.core :as core]
            [goog.events :as events]))

(if (= js/location.pathname "/login")
  (if-not @login/system
    (login/start)
    (do (login/stop) (login/start)))
  (if-not @core/system
    (core/start)
    (do (core/stop) (core/start))))
