(ns simple-kakeibo.middleware
  (:require [ring.util.response :refer [header response redirect content-type]]
            [integrant.core :as ig]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [wrap-access-rules]]))

(defn- unauthorized-handler
  [request metadata]
  (let [current-url (:uri request)]
    (redirect (format "/login?next=%s" current-url))))

(defmethod ig/init-key ::auth-backend [_ _]
  (session-backend {:unauthorized-handler unauthorized-handler}))

(defn wrap-auth [handler]
  #(if (authenticated? %)
     (handler %)
     {:status 401
      :body {:message "Login required."}}))

(defmethod ig/init-key ::wrap-auth [_ {:keys [auth-backend]}]
  (fn [handler]
    (-> handler
        (wrap-auth)
        (wrap-authorization auth-backend)
        (wrap-authentication auth-backend))))
