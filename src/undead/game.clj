(ns undead.game
  (:require [clojure.core.match :refer [match]]))

(defn get-initial-events [seed]
  (let [rng (java.util.Random. seed)]
    [[:added-zombie {:id :zombie-1
                     :kind :biker
                     :health {:max 8 :current 6}}]
     [:set-player-health {:max 9 :current 9}]
     [:added-dice (for [i (range 5)]
                    {:id (keyword (str "die-" i))
                     :faces [:punch :heal :shields :shovel :punches :skull]
                     :current-face (mod (.nextInt rng) 6)})]
     [:set-player-rerolls 2]
     [:set-seed (inc seed)]]))

(defn add-dice [game dice]
  (update game :dice (fnil into {}) (map (juxt :id identity) dice)))

(defn roll-die [game roll]
  (assoc-in game [:dice (:die-id roll) :current-face] (:to roll)))

(defn update-game [game event]
  (match event
    [:added-dice dice] (add-dice game dice)
    [:added-zombie zombie] game
    [:dice-rolled rolls] (reduce roll-die game rolls)
    [:set-die-locked? opts] (assoc-in game [:dice (:die-id opts) :locked?] (:locked? opts))
    [:set-player-health health] game
    [:set-player-rerolls n] (assoc game :rerolls n)
    [:set-seed seed] (assoc game :seed seed)
    [:spent-reroll opt] (assoc game :spent-rerolls (:spent-rerolls opt))))

(defn reroll-allowed? [{:keys [rerolls spent-rerolls]} n]
  (and
   (< -1 n rerolls)
   (not (contains? (set spent-rerolls) n))))

(defn reroll [game n]
  (when (reroll-allowed? game n)
    (let [rng (java.util.Random. (:seed game))]
      [[:spent-reroll {:rerolls (:rerolls game)
                       :spent-rerolls (conj (:spent-rerolls game #{}) n)}]
       [:dice-rolled (for [die (vals (:dice game))]
                       {:die-id (:id die)
                        :from (:current-face die)
                        :to (mod (.nextInt rng) (count (:faces die)))
                        :roll-id (:seed game)})]
       [:set-seed (inc (:seed game))]])))

(defn set-die-locked? [game die-id locked?]
  (when (get-in game [:dice die-id])
    [[:set-die-locked? {:die-id die-id, :locked? locked?}]]))

(defn perform-command [game command]
  (match command
    [:initialize seed] (get-initial-events seed)
    [:reroll n] (reroll game n)
    [:set-die-locked? die-id locked?] (set-die-locked? game die-id locked?)))
