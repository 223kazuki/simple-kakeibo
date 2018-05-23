(ns simple-kakeibo.client.login
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [integrant.core :as ig]
            [soda-ash.core :as sa]
            [cljsjs.semantic-ui-react]
            [ajax.core :as ajax]))

(def default-db
  {:loading false
   :form {}
   :login-result nil})

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   default-db))

(re-frame/reg-event-db
 ::update-form
 (fn [db [_ name update-fn]]
   (update-in db [:form (keyword name)] update-fn)))

(re-frame/reg-event-fx
 ::login
 (fn [{:keys [db]} _]
   {:db   (assoc db :login-result nil :loading true)
    :http-xhrio {:method          :post
                 :uri             "/login"
                 :timeout         5000
                 :params          (:form db)
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::login-post-success]
                 :on-failure      [::login-post-fail]}}))

(re-frame/reg-event-db
 ::login-post-success
 (fn [db v]
   (aset js/window.location "href" "/") db))

(re-frame/reg-event-db
 ::login-post-fail
 (fn [db v]
   (let [message (get-in (second v) [:response :message])]
     (assoc db
            :loading false
            :login-result message))))

(re-frame/reg-sub
 ::form
 (fn [db]
   (:form db)))

(re-frame/reg-sub
 ::login-result
 (fn [db]
   (:login-result db)))

(defn- input-text-handler [el]
  (let [n (aget (.-target el) "name")
        v (aget (.-target el) "value")]
    (re-frame/dispatch [::update-form n (fn [_] v)])))

(defn login-panel []
  (let [form (re-frame/subscribe [::form])
        login-result (re-frame/subscribe [::login-result])]
    [:div {:style {:height "100%"}}
     [sa/Grid {:textAlign "center"
               :style {:height "100%"}
               :verticalAlign "middle"}
      [sa/GridColumn {:style {:maxWidth "450px"}}
       [sa/Header {:as "h2" :color "teal" :textAlign "center"}
        " Login to console"]
       (when @login-result [sa/Message {:as "h3" :color "red" :error true} @login-result])
       [sa/Form {:size "large"}
        [sa/Segment {:stacked true}
         [sa/FormInput {:name "email" :fluid true :icon "user" :iconPosition "left" :placeholder "E-mail address"
                        :onChange input-text-handler}]
         [sa/FormInput {:name "password" :fluid true :icon "lock" :iconPosition "left" :placeholder "Password" :type "password"
                        :onChange input-text-handler}]
         [sa/Button {:color "teal" :fluid true :size "large"
                     :onClick #(re-frame/dispatch [::login])} "Login"]]]]]]))

(defonce system (atom nil))

(defmethod ig/init-key ::db
  [_ _]
  (re-frame/dispatch-sync [::initialize-db]))

(defmethod ig/init-key ::app
  [_ _]
  (re-frame/clear-subscription-cache!)
  (reagent/render [login-panel]
                  (.getElementById js/document "app")))

(def system-conf
  {::db nil
   ::app {:db (ig/ref ::db)}})

(defn start []
  (reset! system (ig/init system-conf)))

(defn stop []
  (ig/halt! @system)
  (reset! system nil))
