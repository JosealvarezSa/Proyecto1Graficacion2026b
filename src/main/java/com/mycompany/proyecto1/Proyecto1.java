package com.mycompany.proyecto1;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
public class Proyecto1 {
    public static class Punto {
        double x, y;
        Punto(double x, double y) { this.x = x; this.y = y; }
    }
    public static class Orientacion {
        public static Punto restar(Punto a, Punto b) {
            return new Punto(a.x - b.x, a.y - b.y);
        }
        public static Punto sumar(Punto a, Punto b) {
            return new Punto(a.x + b.x, a.y + b.y);
        }

        // Calcula el producto cruzado de los vectores AB y AC
        public static double productoCruz(Punto a, Punto b, Punto c) {
            return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
        }

        // 1 = antihorario, -1 = horario, 0 = colineales
        public static int orientacion(Punto a, Punto b, Punto c) {
            double cruz = productoCruz(a, b, c);
            if (cruz > 1e-9) return 1;
            if (cruz < -1e-9) return -1;
            return 0;
        }

        // Revisa que todos los giros del poligono tengan el mismo sentido
        public static boolean esConvexo(List<Punto> v) {
            int n = v.size();
            if (n < 3) return false;

            int sentido = 0;
            for (int i = 0; i < n; i++) {
                Punto a = v.get(i);
                Punto b = v.get((i + 1) % n);
                Punto c = v.get((i + 2) % n);
                int o = orientacion(a, b, c);
                if (o != 0) {
                    if (sentido == 0) {
                        sentido = o;
                    } else if (sentido != o) {
                        return false; // cambio de sentido = concavo
                    }
                }
            }
            return true;
        }

        public static boolean puntoDentroPoligono(List<Punto> v, Punto p) {
            boolean dentro = false;
            int n = v.size();
            for (int i = 0, j = n - 1; i < n; j = i++) {
                Punto vi = v.get(i);
                Punto vj = v.get(j);
                if ((vi.y > p.y) != (vj.y > p.y) &&
                    p.x < (vj.x - vi.x) * (p.y - vi.y) / (vj.y - vi.y) + vi.x) {
                    dentro = !dentro;
                }
            }
            return dentro;
        }
    }
    public static class Poligono {
        private final List<Punto> vertices = new ArrayList<>();
        public void agregarVertice(double x, double y) {
            vertices.add(new Punto(x, y));
        }
        public List<Punto> getVertices() { return vertices; }
        public int cantidad() { return vertices.size(); }

        // Devuelve "Convexo", "Concavo" o "Colineal"
        public String getTipo() {
            if (cantidad() < 3) return "Invalido";

            // Si todos los puntos son colineales
            boolean colineal = true;
            for (int i = 0; i < cantidad(); i++) {
                Punto a = vertices.get(i);
                Punto b = vertices.get((i + 1) % cantidad());
                Punto c = vertices.get((i + 2) % cantidad());
                if (Orientacion.orientacion(a, b, c) != 0) {
                    colineal = false;
                    break;
                }
            }
            if (colineal) return "Colineal";

            return Orientacion.esConvexo(vertices) ? "Convexo" : "Concavo";
        }

        // Devuelve "Antihorario", "Horario" o "Colineal" segun el area firmada
        public String getSentido() {
            if (cantidad() < 3) return "";

            double area = 0;
            int n = vertices.size();
            for (int i = 0; i < n; i++) {
                Punto a = vertices.get(i);
                Punto b = vertices.get((i + 1) % n);
                area += (a.x * b.y - b.x * a.y);
            }

            if (Math.abs(area) < 1e-9) return "Colineal";
            return area > 0 ? "Antihorario" : "Horario";
        }

        // Naranja = convexo, Azul = concavo, Verde = colineal
        public Color getColor() {
            String tipo = getTipo();
            if (tipo.equals("Convexo")) return new Color(255, 140, 0);   // Naranja
            if (tipo.equals("Concavo")) return new Color(70, 130, 255);  // Azul
            if (tipo.equals("Colineal")) return new Color(50, 200, 80);  // Verde
            return Color.WHITE;
        }
    }
    public static class Blanco {
        double cx, cy;
        double vx, vy;
        double radio;
        Poligono poligono;
        boolean vivo = true;
        Blanco(double cx, double cy, double vx, double vy, double radio, Poligono p) {
            this.cx = cx;
            this.cy = cy;
            this.vx = vx;
            this.vy = vy;
            this.radio = radio;
            this.poligono = p;
        }
        void mover(double ancho, double alto) {
            cx += vx;
            cy += vy;
            if (cx - radio < 0 || cx + radio > ancho) {
                vx = -vx;
                cx = Math.max(radio, Math.min(ancho - radio, cx));
            }
            if (cy - radio < 0 || cy + radio > alto) {
                vy = -vy;
                cy = Math.max(radio, Math.min(alto - radio, cy));
            }
            for (Punto p : poligono.getVertices()) {
                p.x += vx;
                p.y += vy;
            }
        }
    }
    public static class Generador {
        private static final Random rnd = new Random();
        private static final double VELOCIDAD = 0.25;
        public static Blanco generarEjemplo(double ancho, double alto) {
            double radio = 2.0;
            double cx = ancho / 2;
            double cy = alto / 2;
            double angulo = rnd.nextDouble() * 2 * Math.PI;
            double vx = Math.cos(angulo) * VELOCIDAD;
            double vy = Math.sin(angulo) * VELOCIDAD;
            Poligono p = new Poligono();
            int lados = 5;
            double inicio = 0;
            for (int i = 0; i < lados; i++) {
                double ang = inicio + 2 * Math.PI * i / lados;
                p.agregarVertice(cx + radio * Math.cos(ang), cy + radio * Math.sin(ang));
            }
            return new Blanco(cx, cy, vx, vy, radio, p);
        }
    }
    // Efectos de tiro (NO MODIFICAR)
    public static class Explosion {
        double x, y;
        int vida = 30;
        int radio = 10;
        Color color;
        Explosion(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }
    public static class TextoFlotante {
        double x, y;
        int vida = 50;
        String texto;
        Color color;
        TextoFlotante(double x, double y, String texto, Color color) {
            this.x = x;
            this.y = y;
            this.texto = texto;
            this.color = color;
        }
    }
    // Panel de juego (NO MODIFICAR)
    public static class PanelJuego extends JPanel {
        private static final double MUNDO_ANCHO = 12;
        private static final double MUNDO_ALTO = 9;
        private final List<Blanco> blancos = new ArrayList<>();
        private final List<Explosion> explosiones = new ArrayList<>();
        private final List<TextoFlotante> textos = new ArrayList<>();
        private int puntaje = 0;
        private int aciertos = 0;
        private int errores = 0;
        private JLabel lblPuntaje, lblAciertos, lblErrores, lblConsigna;
        private Timer timerAnimacion;
        public PanelJuego() {
            setBackground(new Color(15, 20, 35));
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(900, 700));
            construirInterfaz();
            iniciarTimers();
        }
        private void construirInterfaz() {
            JPanel superior = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
            superior.setBackground(new Color(25, 30, 50));
            lblPuntaje = crearEtiqueta("Puntaje: 0", new Color(255, 220, 100));
            lblAciertos = crearEtiqueta("Aciertos: 0", new Color(120, 255, 150));
            lblErrores = crearEtiqueta("Errores: 0", new Color(255, 120, 120));
            superior.add(lblPuntaje);
            superior.add(lblAciertos);
            superior.add(lblErrores);
            JPanel inferior = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            inferior.setBackground(new Color(25, 30, 50));
            lblConsigna = new JLabel("Haz clic DENTRO de los polígonos");
            lblConsigna.setFont(new Font("SansSerif", Font.BOLD, 15));
            lblConsigna.setForeground(new Color(255, 255, 200));
            inferior.add(lblConsigna);
            add(superior, BorderLayout.NORTH);
            add(inferior, BorderLayout.SOUTH);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) { // se cambio de mouseClicked a mousePressed para mejor respuesta
                    disparar(e.getX(), e.getY());
                }
            });
        }
        private JLabel crearEtiqueta(String txt, Color color) {
            JLabel l = new JLabel(txt);
            l.setFont(new Font("SansSerif", Font.BOLD, 14));
            l.setForeground(color);
            return l;
        }
        private void iniciarTimers() {
            timerAnimacion = new Timer(30, e -> {
                for (Blanco b : blancos) {
                    if (b.vivo) b.mover(MUNDO_ANCHO, MUNDO_ALTO);
                }
                for (int i = explosiones.size() - 1; i >= 0; i--) {
                    Explosion ex = explosiones.get(i);
                    ex.radio += 3;
                    ex.vida--;
                    if (ex.vida <= 0) explosiones.remove(i);
                }
                for (int i = textos.size() - 1; i >= 0; i--) {
                    TextoFlotante t = textos.get(i);
                    t.y -= 2;
                    t.vida--;
                    if (t.vida <= 0) textos.remove(i);
                }
                repaint();
            });
            timerAnimacion.start();
        }
        public void agregarBlanco(Blanco b) {
            blancos.add(b);
            repaint();
        }
        private void disparar(int px, int py) {
            Punto p = pantallaAMundo(px, py);
            Blanco impactado = null;
            for (Blanco b : blancos) {
                if (!b.vivo) continue;
                if (Orientacion.puntoDentroPoligono(b.poligono.getVertices(), p)) {
                    impactado = b;
                    break;
                }
            }
            if (impactado != null) {
                impactado.vivo = false;
                blancos.remove(impactado);
                puntaje++;
                aciertos++;
                double[] pant = mundoAPantalla(impactado.cx, impactado.cy);
                explosiones.add(new Explosion(pant[0], pant[1], impactado.poligono.getColor()));
                textos.add(new TextoFlotante(pant[0], pant[1], "+1", new Color(120, 255, 120)));
                lblConsigna.setText("Impacto! +1  (" + impactado.poligono.getTipo()
                        + " " + impactado.poligono.getSentido() + ")");
            } else {
                puntaje--;
                errores++;
                double[] pant = mundoAPantalla(p.x, p.y);
                textos.add(new TextoFlotante(pant[0], pant[1], "-1", new Color(255, 100, 100)));
                explosiones.add(new Explosion(pant[0], pant[1], new Color(255, 100, 100)));
                lblConsigna.setText("¡Fallaste! -1");
            }
            actualizarMarcador();
            repaint();
        }
        private void actualizarMarcador() {
            lblPuntaje.setText("Puntaje: " + puntaje);
            lblAciertos.setText("Aciertos: " + aciertos);
            lblErrores.setText("Errores: " + errores);
        }
        private double[] mundoAPantalla(double x, double y) {
            double w = getWidth();
            double h = getHeight();
            double escala = Math.min(w / MUNDO_ANCHO, h / MUNDO_ALTO);
            return new double[]{x * escala, y * escala};
        }
        private Punto pantallaAMundo(int px, int py) {
            double w = getWidth();
            double h = getHeight();
            double escala = Math.min(w / MUNDO_ANCHO, h / MUNDO_ALTO);
            return new Punto(px / escala, py / escala);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(15, 20, 35));
            g2.fillRect(0, 0, getWidth(), getHeight());
            for (Blanco b : blancos) {
                if (b.vivo) dibujarBlanco(g2, b);
            }
            for (Explosion ex : explosiones) {
                float alpha = ex.vida / 30f;
                g2.setColor(new Color(ex.color.getRed(), ex.color.getGreen(),
                        ex.color.getBlue(), (int)(alpha * 255)));
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval((int) ex.x - ex.radio, (int) ex.y - ex.radio,
                        ex.radio * 2, ex.radio * 2);
            }
            for (TextoFlotante t : textos) {
                float alpha = t.vida / 50f;
                g2.setColor(new Color(t.color.getRed(), t.color.getGreen(),
                        t.color.getBlue(), (int)(alpha * 255)));
                g2.setFont(new Font("SansSerif", Font.BOLD, 22));
                g2.drawString(t.texto, (int) t.x, (int) t.y);
            }
        }
        private void dibujarBlanco(Graphics2D g2, Blanco b) {
            List<Punto> v = b.poligono.getVertices();
            if (v.size() < 3) return;
            Path2D figura = new Path2D.Double();
            double[] p0 = mundoAPantalla(v.get(0).x, v.get(0).y);
            figura.moveTo(p0[0], p0[1]);
            for (int i = 1; i < v.size(); i++) {
                double[] p = mundoAPantalla(v.get(i).x, v.get(i).y);
                figura.lineTo(p[0], p[1]);
            }
            figura.closePath();
            Color color = b.poligono.getColor();
            g2.setColor(new Color(color.getRed(), color.getGreen(),
                                  color.getBlue(), 180));
            g2.fill(figura);
            g2.setColor(color.brighter());
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(figura);
            double[] centro = mundoAPantalla(b.cx, b.cy);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            String txtTipo = b.poligono.getTipo();
            int anchoTipo = g2.getFontMetrics().stringWidth(txtTipo);
            g2.drawString(txtTipo, (int) centro[0] - anchoTipo / 2, (int) centro[1] - 5);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            String txtSentido = b.poligono.getSentido();
            int anchoSentido = g2.getFontMetrics().stringWidth(txtSentido);
            g2.drawString(txtSentido, (int) centro[0] - anchoSentido / 2, (int) centro[1] + 12);
        }
    }

    // Lee las coordenadas del texto (formato x,y por linea)
    // Retorna null si hay menos de 3 puntos validos
    public static Poligono parsearCoordenadas(String texto) {
        if (texto == null || texto.trim().isEmpty()) return null;

        Poligono p = new Poligono();
        String[] lineas = texto.trim().split("\\n");

        for (String linea : lineas) {
            linea = linea.trim();
            if (linea.isEmpty()) continue;

            String[] partes = linea.split(",");
            if (partes.length != 2) continue;

            try {
                double x = Double.parseDouble(partes[0].trim());
                double y = Double.parseDouble(partes[1].trim());
                p.agregarVertice(x, y);
            } catch (NumberFormatException e) {
                // se ignora la linea si no es numero valido
            }
        }

        if (p.cantidad() < 3) return null;
        return p;
    }

    public static void crearYMostrarVentana() {
        JFrame ventana = new JFrame("Proyecto 1 — Polígonos 2D");
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setLayout(new BorderLayout());
        PanelJuego panel = new PanelJuego();
        JPanel lateral = new JPanel(new BorderLayout(5, 5));
        lateral.setBackground(new Color(25, 30, 50));
        lateral.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        lateral.setPreferredSize(new Dimension(280, 0));
        JLabel titulo = new JLabel("➕ Crear polígono");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel instruccion = new JLabel("<html>Escribe las coordenadas<br>(una por línea, formato x, y):</html>");
        instruccion.setForeground(new Color(200, 200, 220));
        instruccion.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JTextArea areaCoords = new JTextArea(8, 20);
        areaCoords.setText("0,0\n2,0\n3,1\n2,2\n0,2");
        areaCoords.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(areaCoords);
        JButton btnAgregar = new JButton("Agregar al juego");
        btnAgregar.setBackground(new Color(100, 200, 120));
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.setFocusPainted(false);
        btnAgregar.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel lblEstado = new JLabel(" ");
        lblEstado.setForeground(new Color(200, 255, 200));
        lblEstado.setFont(new Font("SansSerif", Font.BOLD, 12));

        // Logica del boton: parsea coordenadas, valida y agrega el poligono al juego
        btnAgregar.addActionListener(e -> {
            String texto = areaCoords.getText();
            Poligono poli = parsearCoordenadas(texto);

            if (poli == null) {
                lblEstado.setForeground(new Color(255, 120, 120));
                lblEstado.setText("Error: se necesitan al menos 3 puntos");
                JOptionPane.showMessageDialog(ventana,
                        "Debes ingresar al menos 3 coordenadas validas\n(formato: x,y  una por linea)",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Centroide = promedio de todos los vertices
            double sumX = 0, sumY = 0;
            for (Punto pt : poli.getVertices()) {
                sumX += pt.x;
                sumY += pt.y;
            }
            double cx = sumX / poli.cantidad();
            double cy = sumY / poli.cantidad();

            // Radio = distancia maxima del centro a cualquier vertice
            double radio = 0;
            for (Punto pt : poli.getVertices()) {
                double dx = pt.x - cx;
                double dy = pt.y - cy;
                radio = Math.max(radio, Math.sqrt(dx * dx + dy * dy));
            }
            if (radio < 0.5) radio = 0.5;

            // Velocidad aleatoria en cualquier direccion
            Random rnd = new Random();
            double angulo = rnd.nextDouble() * 2 * Math.PI;
            double velocidad = 0.25;
            double vx = Math.cos(angulo) * velocidad;
            double vy = Math.sin(angulo) * velocidad;

            Blanco nuevo = new Blanco(cx, cy, vx, vy, radio, poli);
            panel.agregarBlanco(nuevo);

            lblEstado.setForeground(new Color(120, 255, 150));
            lblEstado.setText("Agregado: " + poli.getTipo() + " " + poli.getSentido());
        });

        JPanel panelSuperior = new JPanel(new BorderLayout(5, 5));
        panelSuperior.setOpaque(false);
        panelSuperior.add(titulo, BorderLayout.NORTH);
        panelSuperior.add(instruccion, BorderLayout.CENTER);
        JPanel panelBotones = new JPanel(new BorderLayout(5, 5));
        panelBotones.setOpaque(false);
        panelBotones.add(btnAgregar, BorderLayout.NORTH);
        panelBotones.add(lblEstado, BorderLayout.SOUTH);
        lateral.add(panelSuperior, BorderLayout.NORTH);
        lateral.add(scroll, BorderLayout.CENTER);
        lateral.add(panelBotones, BorderLayout.SOUTH);
        ventana.add(lateral, BorderLayout.WEST);
        ventana.add(panel, BorderLayout.CENTER);
        ventana.setSize(1200, 750);
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);
        panel.agregarBlanco(Generador.generarEjemplo(12, 9));
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Proyecto1::crearYMostrarVentana);
    }
}