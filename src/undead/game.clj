(ns undead.game)

(defn get-initial-events [seed]
  (let [rng (java.util.Random. seed)]
    [[:added-zombie {:id :zombie-1
                     :kind :biker
                     :health {:max 8 :current 6}}]
     [:set-player-health {:max 9 :current 9}]
     [:added-dice (for [i (range 5)]
                    {:id (keyword (str "die-" i))
                     :faces [:punch :heal :shields :shovel :punches :skull]
                     :current-face (mod (.nextInt rng) 6)})]]))

