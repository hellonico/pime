(ns hello7
  (:import [javax.swing JFrame Timer JPanel]
           [java.awt Font Color Graphics Graphics2D RenderingHints BasicStroke]
           [java.awt.event MouseAdapter ActionListener]
           [java.awt.geom Arc2D Arc2D$Double]
           [java.awt GraphicsEnvironment]))

;; Timer durations (5m, 10m, 25m)
(def pomodoro-durations [300 600 1500])
;; Target colors for each mode
(def pomodoro-colors [(Color. 70 130 180)  ;; Steel Blue for 5m
                      (Color. 255 165 0)   ;; Orange for 10m
                      (Color. 220 20 60)]) ;; Crimson for 25m

(defn format-time [seconds]
  (format "%02d:%02d" (quot seconds 60) (mod seconds 60)))

;; Interpolate between black and target color (t = 0 → black, t = 1 → target)
(defn lerp-color [^Color target t]
  (let [r (int (* (.getRed target) t))
        g (int (* (.getGreen target) t))
        b (int (* (.getBlue target) t))]
    (Color. r g b)))

(defn -main []
  (let [frame (JFrame. "Pomodoro")
        gd (.getDefaultScreenDevice (GraphicsEnvironment/getLocalGraphicsEnvironment))
        timer-seconds (atom (first pomodoro-durations))
        current-mode (atom 0)
        running? (atom false)
        blink? (atom false)
        countdown-timer (atom nil)
        blink-timer (atom nil)
        last-x (atom nil)
        last-tap (atom 0)]

    ;; Drawing panel
    (let [panel (proxy [JPanel] []
                  (paintComponent [^Graphics g]
                    (proxy-super paintComponent g)
                    (let [g2 (.create ^Graphics2D g)
                          w (int (.getWidth this))
                          h (int (.getHeight this))
                          size (min w h)
                          padding 100 ;; More padding to prevent overlap
                          arc-size (int (- size (* 2 padding)))
                          total-time (nth pomodoro-durations @current-mode)
                          percent (/ @timer-seconds (double total-time))
                          angle (* 360 percent)
                          target-color (nth pomodoro-colors @current-mode)
                          bg (lerp-color target-color (- 1 percent)) ;; fade from black → color
                          text (if (and @running? @blink?) "" (format-time @timer-seconds))]

                      ;; Background fade
                      (.setColor g2 bg)
                      (.fillRect g2 0 0 w h)

                      ;; Progress ring (smaller, centered)
                      (.setColor g2 Color/WHITE)
                      (.setStroke g2 (BasicStroke. 15))
                      (.draw g2 (Arc2D$Double.
                                  (double padding) (double padding)
                                  (double arc-size) (double arc-size)
                                  90 (- angle) Arc2D/OPEN))

                      ;; Centered timer text
                      (.setRenderingHint g2 RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
                      (.setFont g2 (Font. "SansSerif" Font/BOLD 100))
                      (let [metrics (.getFontMetrics g2)
                            text-width (.stringWidth metrics text)
                            text-height (.getAscent metrics)]
                        (.drawString g2 text
                                     (int (- (/ w 2) (/ text-width 2)))
                                     (int (+ (/ h 2) (/ text-height 3)))))
                      (.dispose g2))))]

      ;; Helper to repaint
      (defn repaint! [] (.repaint panel))

      ;; Timers
      (letfn [(start-blink []
                (reset! blink-timer
                        (Timer. 500
                                (proxy [ActionListener] []
                                  (actionPerformed [_]
                                    (swap! blink? not)
                                    (repaint!)))))
                (.start ^Timer @blink-timer))

              (start-timer []
                (reset! running? true)
                (reset! countdown-timer
                        (Timer. 1000
                                (proxy [ActionListener] []
                                  (actionPerformed [_]
                                    (if (pos? @timer-seconds)
                                      (do
                                        (swap! timer-seconds dec)
                                        (repaint!))
                                      (do
                                        (.stop ^Timer @countdown-timer)
                                        (reset! running? false)
                                        (start-blink)))))))
                (.start ^Timer @countdown-timer))

              (switch-mode [dir]
                (reset! current-mode (mod (+ @current-mode dir) (count pomodoro-durations)))
                (reset! timer-seconds (nth pomodoro-durations @current-mode))
                (reset! running? false)
                (reset! blink? false)
                (when-let [t @countdown-timer] (.stop ^Timer t))
                (when-let [b @blink-timer] (.stop ^Timer b))
                (repaint!))]

        ;; Gesture detection
        (let [touch-handler
              (proxy [MouseAdapter] []
                (mousePressed [e]
                  (reset! last-x (.getX e))
                  (let [now (System/currentTimeMillis)]
                    (when (< (- now @last-tap) 300)
                      (when (not @running?)
                        (start-timer)))
                    (reset! last-tap now)))
                (mouseReleased [e]
                  (let [dx (- (.getX e) @last-x)]
                    (cond
                      (> dx 50) (switch-mode -1)
                      (< dx -50) (switch-mode 1)))))]
          (.addMouseListener panel touch-handler)
          (.addMouseMotionListener panel touch-handler))

        ;; Frame setup
        (.add frame panel)
        (.setUndecorated frame true)
        (.setVisible frame true)
        (.setFullScreenWindow gd frame)
        (repaint!)))))
