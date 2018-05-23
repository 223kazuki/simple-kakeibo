(ns simple-kakeibo.client.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [simple-kakeibo.client.subs :as subs]
            [simple-kakeibo.client.events :as events]
            [soda-ash.core :as sa]
            [cljsjs.semantic-ui-react]
            [cljsjs.react-transition-group]
            [cljs.core.async :refer [timeout <!]]
            [cljsjs.react-datepicker]
            [cljsjs.recharts])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def date-picker (reagent/adapt-react-class js/DatePicker))

(defn- input-text-handler [el]
  (let [n (aget (.-target el) "name")
        v (aget (.-target el) "value")]
    (re-frame/dispatch [::events/update-form n (fn [_] v)])))

(defn- input-calendar-handler [name date]
  (js/console.log (.unix date))
  (re-frame/dispatch [::events/update-form name (fn [_] date)]))

(defn wallets-view []
  (let [wallets (re-frame/subscribe [::subs/wallets])]
    [sa/Segment
     (when (nil? @wallets)
       [sa/Dimmer {:active true}
        [sa/Loader "Loading..."]])
     [sa/Header {:as "h2" :textAlign "center"}
      "Wallets"]
     [sa/Table {:celled true :style {:width "100%"}}
      [sa/TableHeader
       [sa/TableRow
        [sa/TableHeaderCell {:width 4} "Name"]
        [sa/TableHeaderCell {:width 4} "Ammount"]
        [sa/TableHeaderCell {:width 3}]]]
      [sa/TableBody
       (for [{:keys [id name ammount]} @wallets]
         [sa/TableRow {:key id}
          [sa/TableCell name]
          [sa/TableCell
           [sa/FormInput { :labelPosition "left" :fluid true}
            [sa/Label {:basic true} "$"]
            [:input {:type "number" :name (str "wallet." id "/ammount") :placeholder "Ammount" :defaultValue ammount
                     :onChange input-text-handler}]]]
          [sa/TableCell {:textAlign "center"}
           [sa/Button {:primary true
                       :onClick #(let [form (re-frame/subscribe [::subs/form])
                                       ammount (get-in @form [(keyword (str "wallet." id "/ammount"))])]
                                   (re-frame/dispatch [::events/update-wallet id ammount]))}
            "Update"]
           [sa/Button {:primary true :negative true :onClick #(re-frame/dispatch [::events/delete-wallet id])}
            "Delete"]]])
       [sa/TableRow
        [sa/TableCell
         [sa/FormInput {:name "wallet/name" :fluid true :placeholder "Name"
                        :onChange input-text-handler}]]
        [sa/TableCell
         [sa/FormInput { :labelPosition "left" :fluid true}
          [sa/Label {:basic true} "$"]
          [:input {:type "number" :name "wallet/ammount" :placeholder "Ammount"
                   :onChange input-text-handler}]]]
        [sa/TableCell {:textAlign "center"}
         [sa/Button {:primary true
                     :onClick #(re-frame/dispatch [::events/create-wallet])}
          "Add"]]]]]]))

(defn payments-view []
  (let [payments (re-frame/subscribe [::subs/payments])
        form (re-frame/subscribe [::subs/form])]
    [sa/Segment
     (when (nil? @payments)
       [sa/Dimmer {:active true}
        [sa/Loader "Loading..."]])
     [sa/Header {:as "h2" :textAlign "center"}
      "Payments"]
     [sa/Table {:celled true :style {:width "100%"}}
      [sa/TableHeader
       [sa/TableRow
        [sa/TableHeaderCell {:width 1}]
        [sa/TableHeaderCell {:width 3} "Name"]
        [sa/TableHeaderCell {:width 3} "Ammount"]
        [sa/TableHeaderCell {:width 2} "Due Date"]
        [sa/TableHeaderCell {:width 3}]]]
      [sa/TableBody
       (for [{:keys [id name ammount due_date_unix settled?] :as aaa} @payments]
         [sa/TableRow {:key id}
          [sa/TableCell {:textAlign "center"} [sa/Checkbox {:onChange #(if settled?
                                                                         (re-frame/dispatch [::events/unsettle-payment id])
                                                                         (re-frame/dispatch [::events/settle-payment id]))
                                                            :checked settled?}]]
          [sa/TableCell name]
          [sa/TableCell
           [sa/FormInput {:labelPosition "left" :fluid true}
            [sa/Label {:basic true} "$"]
            [:input {:type "number" :name (str "payment." id "/ammount") :placeholder "Ammount" :defaultValue ammount
                     :onChange input-text-handler}]]]
          [sa/TableCell (.format (.unix js/moment due_date_unix) "MM/DD/YYYY")]
          [sa/TableCell {:textAlign "center"}
           [sa/Button {:primary true
                       :onClick #(let [form (re-frame/subscribe [::subs/form])
                                       ammount (get-in @form [(keyword (str "payment." id "/ammount"))])]
                                   (re-frame/dispatch [::events/update-payment id ammount]))}
            "Update"]
           [sa/Button {:primary true :negative true :onClick #(re-frame/dispatch [::events/delete-payment id])}
            "Delete"]]])
       [sa/TableRow
        [sa/TableCell]
        [sa/TableCell
         [sa/FormInput {:name "payment/name" :fluid true :placeholder "Name"
                        :onChange input-text-handler}]]
        [sa/TableCell
         [sa/FormInput { :labelPosition "left" :fluid true}
          [sa/Label {:basic true} "$"]
          [:input {:type "number" :name "payment/ammount" :placeholder "Ammount"
                   :onChange input-text-handler}]]]
        [sa/TableCell
         [date-picker {:selected (:payment/due_date @form) :onChange #(input-calendar-handler "payment/due_date" %)}]]
        [sa/TableCell {:textAlign "center"}
         [sa/Button {:primary true
                     :onClick #(re-frame/dispatch [::events/create-payment])}
          "Add"]]]]]]))

(def composed-chart (reagent/adapt-react-class js/Recharts.ComposedChart))
(def x-axis (reagent/adapt-react-class js/Recharts.XAxis))
(def y-axis (reagent/adapt-react-class js/Recharts.YAxis))
(def z-axis (reagent/adapt-react-class js/Recharts.ZAxis))
(def tooltip (reagent/adapt-react-class js/Recharts.Tooltip))
(def legend (reagent/adapt-react-class js/Recharts.Legend))
(def cartesian-grid (reagent/adapt-react-class js/Recharts.CartesianGrid))
(def area (reagent/adapt-react-class js/Recharts.Area))
(def bar (reagent/adapt-react-class js/Recharts.Bar))
(def scatter-chart (reagent/adapt-react-class js/Recharts.ScatterChart))
(def scatter (reagent/adapt-react-class js/Recharts.Scatter))

(defn chart-view []
  (let [payments (re-frame/subscribe [::subs/payments])
        wallets  (re-frame/subscribe [::subs/wallets])
        payments (->> @payments
                      (sort-by :due_date_unix))
        transitions (let [current {:ammount (->> @wallets
                                                 (map :ammount)
                                                 (reduce +))
                                   :date_unix (->> @wallets
                                                   (map :charges)
                                                   flatten
                                                   (map :event_date_unix)
                                                   sort
                                                   last)}]
                      (->> @wallets
                           (map :charges)
                           flatten
                           (sort-by :event_date_unix)
                           reverse
                           (reduce (fn [result data]
                                     (let [pre-data (first result)]
                                       (println pre-data data)
                                       (cons {:ammount (- (:ammount pre-data) (:charge data))
                                              :date_unix (:event_date_unix data)} result)))
                                   [current])))]
    (js/console.log (pr-str payments))
    (js/console.log (pr-str transitions))
    [:div
     [scatter-chart {:width 600 :height 280 :margin {:top 20 :right 60 :bottom 0 :left 0}}
      [cartesian-grid]
      [x-axis {:type "number" :dataKey "date_unix" :name "date" :domain ["auto" "auto"] :unit "" :tickFormatter (fn [t] (.format (js/moment t) "MM/DD"))}]
      [y-axis {:type "number" :dataKey "ammount" :name "ammount" :unit "$"}]
      [scatter {:name "A" :data (clj->js transitions) :fill "#8884d8" :line true}]]
     ;; [composed-chart {:width 600 :height 280 :data (clj->js data-graph) :margin {:top 20 :right 60 :bottom 0 :left 0}}
     ;;  [x-axis {:dataKey "month"}]
     ;;  [y-axis]
     ;;  [cartesian-grid {:stroke "#f5f5f5"}]
     ;;  [area {:type "monotone" :dataKey "総売上" :stroke "#f5f5f5" :fillOpacity 1 :fill "rgba(0, 172, 237, 0.2)"}]
     ;;  [bar {:dataKey "売上" :barSize 20 :stroke "rgba(34, 80, 162, 0.2)" :fillOpacity 1 :fille "#2250A2j"}]]
     ]))

(defn home-panel []
  (let [paymets []
        result nil]
    [:div
     (when-let  [{:keys [type message]} nil]
       (if (= type :failure)
         [sa/Message {:as "h3" :color "red" :error true} message]
         [sa/Message {:as "h3" :color "blue"} message]))
     [chart-view]
     [wallets-view]
     [payments-view]]))

(defn about-panel []
  [:div "About"])

(defn none-panel []
  [:div])

(defmulti panels identity)
(defmethod panels :home-panel [] #'home-panel)
(defmethod panels :about-panel [] #'about-panel)
(defmethod panels :none [] #'none-panel)
(defmethod panels :default [] [:div "This page does not exist."])

(def transition-group
  (reagent/adapt-react-class js/ReactTransitionGroup.TransitionGroup))
(def css-transition
  (reagent/adapt-react-class js/ReactTransitionGroup.CSSTransition))

(defn main-panel []
  (reagent/create-class
   {:component-did-mount
    (fn []
      (re-frame/dispatch [::events/fetch-wallets])
      (re-frame/dispatch [::events/fetch-payments]))

    :reagent-render
    (fn []
      (let [active-panel (re-frame/subscribe [::subs/active-panel])]
        [:div
         [sa/Menu {:fixed "top" :inverted true}
          [sa/Container
           [sa/MenuItem {:as "a" :header true  :href "/"}
            "Simple Kakeibo"]
           [sa/MenuItem {:as "a" :href "/about"} "About"]
           [sa/MenuItem {:position "right"}
            [sa/Button {:as "a" :href "/logout" :inverted true} "Logout"]]]]
         [sa/Container {:className "mainContainer" :style {:marginTop "7em"}}
          [transition-group
           [css-transition {:key @active-panel
                            :classNames "pageChange"
                            :timeout 500
                            :className "transition"}
            [(panels @active-panel)]]]]]))}))
