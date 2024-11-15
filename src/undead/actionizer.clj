(ns undead.actionizer
  (:require [clojure.core.match :refer [match]]))

(defn balance-hearts [hearts]
  (let [heart-count (count hearts)
        num-lines (Math/ceil (/ heart-count 6))]
    (partition-all (/ heart-count num-lines) hearts)))

(defn render-hearts [health]
  (concat (repeat (:current health) :heart)
          (repeat (- (:max health)
                     (:current health)) :lost)))

(defn prepare-hearts [health]
  (balance-hearts (render-hearts health)))

(defn prepare-zombie [zombie]
  {:kind (:kind zombie)
   :on-click [:finish-turn {:target (:id zombie)}]
   :hearts (prepare-hearts (:health zombie))
   :intention-classes nil})

(defn add-zombie [zombie]
  [[:assoc-in [:zombies (:id zombie)] (prepare-zombie zombie)]])

(defn set-player-health [health]
  [[:assoc-in [:player :hearts] (render-hearts health)]])

(defn render-faces [faces]
  (map-indexed
   (fn [i face]
     [(str "face-" i) face])
   faces))

(defn add-dice [dice]
  (concat
   (for [die dice]
     [:assoc-in [:dice (:id die)]
      {:faces (render-faces (:faces die))
       :lock-command [:set-die-locked? (:id die) true]
       :die-class "entering"
       :cube-class (str "entering-" (:current-face die))}])
   [[:wait 1800]]))

(defn prepare-rerolls [opts]
  [[:assoc-in [:player :rerolls]
    (for [i (range (:rerolls opts))]
      (if (contains? (:spent-rerolls opts) i)
        {:used? true}
        {:on-click [:reroll i]}))]])

(defn replenish-rerolls [opts]
  [[:assoc-in [:player :rerolls]
    (for [i (range (:rerolls opts))]
      {:on-click [:reroll i]})]])

(defn roll-dice [rolls]
  (concat
   (mapcat
    (fn [{:keys [die-id from to roll-id]}]
      [[:assoc-in [:dice die-id :die-class] "rolling"]
       [:assoc-in [:dice die-id :cube-class] (str "roll-" from "-to-" to)]
       [:assoc-in [:dice die-id :key] (str (name die-id) "-" roll-id)]])
    rolls)
   [[:wait 1800]]))

(defn set-die-locked? [{:keys [die-id locked?]}]
  [[:assoc-in [:dice die-id :clamp-class] (when locked? "locked")]
   [:assoc-in [:dice die-id :lock-command] [:set-die-locked? die-id (not locked?)]]])

(def punch-classes (cycle ["punched-3" "punched-1" "punched-4" "punched-2" "punched-5"]))

(defn punch-zombie [{:keys [zombie-id damage die-ids health]}]
  (concat
   (for [id die-ids]
     [:assoc-in [:dice id :die-class] "using"])
   (mapcat (fn [class i]
             [[:assoc-in [:zombies zombie-id :class] class]
              [:assoc-in [:zombies zombie-id :hearts]
               (prepare-hearts (update health :current - 1 i))]
              [:wait 200]])
           (take damage punch-classes)
           (range))
   (for [id die-ids]
     [:assoc-in [:dice id :die-class] "used"])))

(defn punch-player [{:keys [damage health]}]
  (mapcat (fn [class i]
            [[:assoc-in [:player :class] class]
             [:assoc-in [:player :hearts]
              (render-hearts (update health :current - 1 i))]
             [:wait 200]])
          (take damage punch-classes)
          (range)))

(defn kill-zombie [target]
  [[:assoc-in [:zombies target :class] "falling"]
   [:wait 700]
   [:dissoc-in [:zombies] target]])

(defn plan-zombie-moves [zombie-plans]
  (for [[id intentions] zombie-plans]
    [:assoc-in [:zombies id :intention-classes] intentions]))

(defn set-player-shields [{:keys [value die-ids]}]
  (concat
   (for [id die-ids]
     [:assoc-in [:dice id :die-class] "using"])
   (if (= value 0)
     [[:assoc-in [:player :shields] 0]]
     (mapcat
      (fn [i]
        [[:assoc-in [:player :shields] (inc i)]
         [:wait 70]])
      (range value)))
   (for [id die-ids]
     [:assoc-in [:dice id :die-class] "used"])))

(defn event->actions [event]
  (match event
    [:added-dice dice] (add-dice dice)
    [:added-zombie zombie] (add-zombie zombie)
    [:dice-rolled rolls] (roll-dice rolls)
    [:killed-zombie target] (kill-zombie target)
    [:killed-player] [[:assoc-in [:game-over?] true]]
    [:punched-zombie opts] (punch-zombie opts)
    [:punched-player opts] (punch-player opts)
    [:set-die-locked? opts] (set-die-locked? opts)
    [:set-player-health health] (set-player-health health)
    [:set-player-rerolls n] (prepare-rerolls {:rerolls n})
    [:set-player-shields opts] (set-player-shields opts)
    [:set-seed seed] nil
    [:spent-reroll opts] (prepare-rerolls opts)
    [:started-round opts] [[:assoc-in [:round-number] (:round-number opts)]]
    [:replenished-rerolls opts] (replenish-rerolls opts)
    [:zombies-planned-their-moves opts] (plan-zombie-moves opts)))
