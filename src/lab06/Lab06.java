 package lab06;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class Lab06 extends JFrame {
    private static final int NUM_AUTOS = 10;
    private static final int PISTA_ANCHO = 800;
    private static final int PISTA_ALTO = 400;
    private static final int CAR_WIDTH = 60;
    private static final int CAR_HEIGHT = 30;

    private RacePanel racePanel;
    private JButton btnStart, btnReset;
    private DefaultTableModel resultadoModel;

    static class Resultado {
        int id;
        long tiempoMs;
        Resultado(int id, long ms){ this.id=id; this.tiempoMs=ms;}
    }

    private List<Resultado> resultados = Collections.synchronizedList(new ArrayList<>());

    public Lab06(){
        super("Carrera de Autos - Hilos");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());

        racePanel = new RacePanel(NUM_AUTOS, PISTA_ANCHO, PISTA_ALTO);
        add(racePanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        btnStart = new JButton("Iniciar Carrera");
        btnReset = new JButton("Reiniciar");
        bottom.add(btnStart);
        bottom.add(btnReset);

        resultadoModel = new DefaultTableModel(new String[]{"Posicion","Auto ID","Tiempo (s)"} ,0);
        JTable tablaRes = new JTable(resultadoModel);
        JScrollPane sp = new JScrollPane(tablaRes);
        sp.setPreferredSize(new Dimension(260, PISTA_ALTO));

        JPanel east = new JPanel(new BorderLayout());
        east.add(sp, BorderLayout.CENTER);
        east.add(bottom, BorderLayout.SOUTH);
        add(east, BorderLayout.EAST);

        btnStart.addActionListener(e -> startRace());
        btnReset.addActionListener(e -> resetRace());

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void startRace(){
        btnStart.setEnabled(false);
        resultados.clear();
        resultadoModel.setRowCount(0);
        racePanel.startRace(new RacePanel.FinishCallback(){
            @Override
            public void onFinish(int id, long tiempoMs) {
                resultados.add(new Resultado(id, tiempoMs));
                if(resultados.size() == NUM_AUTOS){
                    resultados.sort(Comparator.comparingLong(r -> r.tiempoMs));
                    SwingUtilities.invokeLater(() -> {
                        resultadoModel.setRowCount(0);
                        int pos = 1;
                        for(Resultado r: resultados){
                            resultadoModel.addRow(new Object[]{pos++, r.id, String.format("%.3f", r.tiempoMs/1000.0)});
                        }
                        btnStart.setEnabled(true);
                    });
                }
            }
        });
    }

    private void resetRace(){
        racePanel.resetPositions();
        resultados.clear();
        resultadoModel.setRowCount(0);
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(Lab06::new);
    }

    static class RacePanel extends JPanel {
        interface FinishCallback { void onFinish(int id, long tiempoMs); }

        private int numAutos;
        private int ancho, alto;
        private int[] xPositions;
        private int[] yPositions;
        private volatile boolean running = false;
        private FinishCallback callback;
        private AtomicInteger finishedCount = new AtomicInteger(0);

        public RacePanel(int numAutos, int ancho, int alto){
            this.numAutos = numAutos;
            this.ancho = ancho;
            this.alto = alto;
            setPreferredSize(new Dimension(ancho, alto));
            xPositions = new int[numAutos];
            yPositions = new int[numAutos];
            resetPositions();
        }

        public void resetPositions(){
            running = false;
            finishedCount.set(0);
            for(int i=0;i<numAutos;i++){
                xPositions[i] = 0;
                yPositions[i] = 20 + i * 35;
            }
            repaint();
        }

        public void startRace(FinishCallback cb){
            if(running) return;          
            this.callback = cb;
            resetPositions();
            finishedCount.set(0);
            running = true;
            for(int i=0;i<numAutos;i++){
                final int id = i+1;
                Thread t = new Thread(() -> runCar(id));
                t.start();
            }
        }

        private void runCar(int id){
            int idx = id-1;
            int meta = ancho - CAR_WIDTH - 10;
            Random rnd = new Random();
            long start = System.currentTimeMillis();
            while(xPositions[idx] < meta && running){
                int avance = 1 + rnd.nextInt(6);
                xPositions[idx] += avance;
                SwingUtilities.invokeLater(this::repaint);
                try {
                    int sleep = 20 + rnd.nextInt(60);
                    Thread.sleep(sleep);
                } catch (InterruptedException e){
                    break;
                }
            }
            long end = System.currentTimeMillis();
            long tiempo = end - start;
            xPositions[idx] = Math.min(xPositions[idx], meta);
            SwingUtilities.invokeLater(this::repaint);
            if(callback != null) callback.onFinish(id, tiempo);
            int finished = finishedCount.incrementAndGet();
            if(finished >= numAutos){
                running = false;
            }
        }

        @Override
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            g.setColor(Color.white);
            g.fillRect(0,0,getWidth(),getHeight());
            for(int i=0;i<numAutos;i++){
                g.setColor(Color.lightGray);
                g.fillRect(0, yPositions[i]-5, getWidth(), CAR_HEIGHT+10);
                g.setColor(Color.getHSBColor((float)i/numAutos, 0.6f, 0.9f));
                g.fillRect(xPositions[i], yPositions[i], CAR_WIDTH, CAR_HEIGHT);
                g.setColor(Color.black);
                g.drawRect(xPositions[i], yPositions[i], CAR_WIDTH, CAR_HEIGHT);
                g.drawString("Auto " + (i+1), xPositions[i]+6, yPositions[i]+18);
            }
            g.setColor(Color.red);
            g.fillRect(ancho - 8, 0, 8, getHeight());
        }
    }
}


