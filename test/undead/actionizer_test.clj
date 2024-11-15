(ns undead.actionizer-test
  (:require [clojure.test :refer [deftest is testing]]
            [undead.actionizer :as sut]))

(deftest actionize--added-zombie
  (testing "adding zombies"
    (is (= (sut/event->actions
            [:added-zombie {:id :zombie-1
                            :kind :biker
                            :health {:max 4 :current 4}}])
           [[:assoc-in [:zombies :zombie-1]
             {:kind :biker
              :hearts [[:heart :heart :heart :heart]]
              :on-click [:finish-turn {:target :zombie-1}]
              :intention-classes nil}]])))

  (testing "lost hearts"
    (is (= (-> (sut/event->actions
                [:added-zombie {:id :zombie-1
                                :kind :biker
                                :health {:max 4 :current 3}}])
               first last :hearts)
           [[:heart :heart :heart :lost]])))

  (testing "balance hearts"
    (is (= (-> (sut/event->actions
                [:added-zombie {:id :zombie-1
                                :kind :biker
                                :health {:max 9 :current 8}}])
               first last :hearts)
           [[:heart :heart :heart :heart :heart]
            [:heart :heart :heart :lost]]))))

(deftest actionize--set-player-health
  (is (= (sut/event->actions
          [:set-player-health {:max 3, :current 3}])
         [[:assoc-in [:player :hearts] [:heart :heart :heart]]]))

  (is (= (sut/event->actions
          [:set-player-health {:max 3, :current 2}])
         [[:assoc-in [:player :hearts] [:heart :heart :lost]]])))

(deftest actionize--added-dice
  (is (= (sut/event->actions
          [:added-dice [{:id :die-1
                         :faces [:punch :heal :shields :shovel :punches :skull]
                         :current-face 2}]])
         [[:assoc-in [:dice :die-1] {:faces [["face-0" :punch]
                                             ["face-1" :heal]
                                             ["face-2" :shields]
                                             ["face-3" :shovel]
                                             ["face-4" :punches]
                                             ["face-5" :skull]]
                                     :lock-command [:set-die-locked? :die-1 true]
                                     :die-class "entering"
                                     :cube-class "entering-2"}]
          [:wait 1800]])))

(deftest actionize--set-player-rerolls
  (is (= (sut/event->actions
          [:set-player-rerolls 2])
         [[:assoc-in [:player :rerolls] [{:on-click [:reroll 0]}
                                         {:on-click [:reroll 1]}]]])))

(deftest actionize--spend-reroll
  (is (= (sut/event->actions
          [:spent-reroll {:rerolls 2
                          :spent-rerolls #{1}}])
         [[:assoc-in [:player :rerolls] [{:on-click [:reroll 0]}
                                         {:used? true}]]])))

(deftest actionize--replenish-rerolls
  (is (= (sut/event->actions
          [:replenished-rerolls {:rerolls 2}])
         [[:assoc-in [:player :rerolls] [{:on-click [:reroll 0]}
                                         {:on-click [:reroll 1]}]]])))

(deftest actionize--roll-dice
  (is (= (sut/event->actions
          [:dice-rolled [{:die-id :die-0
                          :from 1
                          :to 2
                          :roll-id 0}
                         {:die-id :die-1
                          :from 2
                          :to 4
                          :roll-id 0}]])
         [[:assoc-in [:dice :die-0 :die-class] "rolling"]
          [:assoc-in [:dice :die-0 :cube-class] "roll-1-to-2"]
          [:assoc-in [:dice :die-0 :key] "die-0-0"]
          [:assoc-in [:dice :die-1 :die-class] "rolling"]
          [:assoc-in [:dice :die-1 :cube-class] "roll-2-to-4"]
          [:assoc-in [:dice :die-1 :key] "die-1-0"]
          [:wait 1800]])))

(deftest actionize-set-die-locked?
  (is (= (sut/event->actions
          [:set-die-locked? {:die-id :die-0
                             :locked? true}])
         [[:assoc-in [:dice :die-0 :clamp-class] "locked"]
          [:assoc-in [:dice :die-0 :lock-command] [:set-die-locked? :die-0 false]]]))

  (is (= (sut/event->actions
          [:set-die-locked? {:die-id :die-0
                             :locked? false}])
         [[:assoc-in [:dice :die-0 :clamp-class] nil]
          [:assoc-in [:dice :die-0 :lock-command] [:set-die-locked? :die-0 true]]])))

(deftest actionize--punched-zombie
  (is (= (sut/event->actions
          [:punched-zombie {:zombie-id :zombie-1
                            :damage 2
                            :die-ids #{:die-0 :die-1}
                            :health {:max 4 :current 3}}])
         [[:assoc-in [:dice :die-0 :die-class] "using"]
          [:assoc-in [:dice :die-1 :die-class] "using"]
          [:assoc-in [:zombies :zombie-1 :class] "punched-3"]
          [:assoc-in [:zombies :zombie-1 :hearts] [[:heart :heart :lost :lost]]]
          [:wait 200]
          [:assoc-in [:zombies :zombie-1 :class] "punched-1"]
          [:assoc-in [:zombies :zombie-1 :hearts] [[:heart :lost :lost :lost]]]
          [:wait 200]
          [:assoc-in [:dice :die-0 :die-class] "used"]
          [:assoc-in [:dice :die-1 :die-class] "used"]])))

(deftest actionize--killed-zombie
  (is (= (sut/event->actions
          [:killed-zombie :zombie-2])
         [[:assoc-in [:zombies :zombie-2 :class] "falling"]
          [:wait 700]
          [:dissoc-in [:zombies] :zombie-2]])))

(deftest actionize--started-round
  (is (= (sut/event->actions
          [:started-round {:round-number 3}])
         [[:assoc-in [:round-number] 3]])))

(deftest actionize--zombies-planned-their-moves
  (is (= (sut/event->actions
          [:zombies-planned-their-moves
           {:zombie-0 [:punch :punch]}])
         [[:assoc-in [:zombies :zombie-0 :intention-classes] [:punch :punch]]])))

(deftest actionize--punched-player
  (is (= (sut/event->actions
          [:punched-player {:damage 2, :health {:max 4, :current 3}}])
         [[:assoc-in [:player :class] "punched-3"]
          [:assoc-in [:player :hearts] [:heart :heart :lost :lost]]
          [:wait 200]
          [:assoc-in [:player :class] "punched-1"]
          [:assoc-in [:player :hearts] [:heart :lost :lost :lost]]
          [:wait 200]])))

(deftest actionize--killed-player
  (is (= (sut/event->actions [:killed-player])
         [[:assoc-in [:game-over?] true]])))

(deftest actionize--set-player-shields
  (is (= (sut/event->actions [:set-player-shields {:value 2, :die-ids #{:die-3}}])
         [[:assoc-in [:dice :die-3 :die-class] "using"]
          [:assoc-in [:player :shields] 1]
          [:wait 70]
          [:assoc-in [:player :shields] 2]
          [:wait 70]
          [:assoc-in [:dice :die-3 :die-class] "used"]]))

  (is (= (sut/event->actions [:set-player-shields {:value 0}])
         [[:assoc-in [:player :shields] 0]])))