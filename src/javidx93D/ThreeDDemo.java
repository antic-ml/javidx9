package javidx93D;

import java.io.IOException;
import java.io.InputStream;
import java.io.StreamTokenizer;
import java.util.ArrayList;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * A simple 3D object model display. This is a JavaFX implementation of
 * the 3D engine developed by javidx9 in C++. This implementation only covers the
 * first two videos made by javidx9 and as such knows just enough to import a
 * 3D model defined in a .obj file and display it shaded on screen rotating through the
 * x and z axes.
 * 
 * For the complete 3D engine video series see: Code it yourself! 3D Graphics Engine
 * Part #1 - Triangles and Projection (https://www.youtube.com/watch?v=ih20l3pJoeU).
 * 
 * @author Mario Gianota gianotamario@gmail.com
 *
 */
public class ThreeDDemo extends Application {

	// The width of our display
	int width;
	// The height of our display
	int height;

	
	/* A cube object */
	Mesh cube;
	Mesh ship;
	
	/* Projection matrix */
	double[][] matProj;
	
	/* Camera position */
	Vector camera = new Vector(0,0,0);
	
	/* A constant used by the map() method */
	final static double EPSILON = 1e-12;
	
	@Override
	public void start(Stage stage) {
		stage.setTitle( "javidx9 3D Engine" );
		 
	    Group root = new Group();
	    Scene theScene = new Scene( root );
	    stage.setScene( theScene );
	    width = 640;
	    height = 480;
	    
	    Canvas canvas = new Canvas( width, height );
	    root.getChildren().add( canvas );
	 
	    GraphicsContext gc = canvas.getGraphicsContext2D();
	 
	    init();
	    
	    final long startNanoTime = System.nanoTime();
	    // Use an AnimationTimer to run the main loop
	    new AnimationTimer() {
	        public void handle(long currentNanoTime) {
	        	double elapsedTimeInSeconds = (currentNanoTime - startNanoTime) / 1000000000.0;
	        	
	        	draw(gc, elapsedTimeInSeconds);
	        }
	    }.start();
	 
	    stage.show();	
	}
	
	public void init() {
		// Create model from object file definition
		ship = new Mesh();
		boolean loaded = ship.loadFromObjectFile("/obj/teapot.obj");
		if( ! loaded )
			System.out.println("Failed to load model object file.");

		// Projection matrix
		double near = 0.1;
		double far = 1000.0;
		double fov = 90.0;
		double aspectRatio = (double)height/ (double) width;
		double fovRad = 1.0 / Math.tan(fov * 0.5 / 180.0 * 3.14159);
	
		matProj = makeProjectionMatrix(near, far, fov, aspectRatio, fovRad);
		
	}
	public void draw(GraphicsContext g, double elapsedTime) {
		// Clear display to black
		g.setFill(Color.BLACK);
		g.fillRect(0, 0, width, height);

		/* Rotation matrices */
		double theta = 2.0 * elapsedTime;
		double[][] matRotZ = makeRotationZ(theta * 0.5);
		double[][] matRotX = makeRotationX(theta);
		
		// Handle rotation, projection & scaling for each triangle
		ArrayList<Triangle> trianglesToRaster = new ArrayList<>();
		for(int i=0; i<ship.tris.size(); i++) {
			Triangle t = ship.tris.get(i);
			Triangle triProjected = t.clone();

			// Rotate along Z axis
			Triangle triRotatedZ = new Triangle();
			triRotatedZ.p[0] = multiplyMatrixVector(t.p[0], matRotZ);
			triRotatedZ.p[1] = multiplyMatrixVector(t.p[1], matRotZ);
			triRotatedZ.p[2] = multiplyMatrixVector(t.p[2], matRotZ);
			
			// Rotate along X axis
			Triangle triRotatedZX = new Triangle();
			triRotatedZX.p[0] = multiplyMatrixVector(triRotatedZ.p[0], matRotX);
			triRotatedZX.p[1] = multiplyMatrixVector(triRotatedZ.p[1], matRotX);
			triRotatedZX.p[2] = multiplyMatrixVector(triRotatedZ.p[2], matRotX);
			
			
			// Translate
			Triangle triTranslated = triRotatedZX.clone();
			triTranslated.p[0].z += 8.0;
			triTranslated.p[1].z += 8.0;
			triTranslated.p[2].z += 8.0;
			
			// Surface normals
			Vector normal = new Vector(0,0);
			Vector line1 = new Vector(0,0);
			Vector line2 = new Vector(0,0);
			
			line1.x = triTranslated.p[1].x - triTranslated.p[0].x;
			line1.y = triTranslated.p[1].y - triTranslated.p[0].y;
			line1.z = triTranslated.p[1].z - triTranslated.p[0].z;
			
			line2.x = triTranslated.p[2].x - triTranslated.p[0].x;
			line2.y = triTranslated.p[2].y - triTranslated.p[0].y;
			line2.z = triTranslated.p[2].z - triTranslated.p[0].z;
						
			normal.x = line1.y * line2.z - line1.z * line2.y;
			normal.y = line1.z * line2.x - line1.x * line2.z;
			normal.z = line1.x * line2.y - line1.y * line2.x;
			
			normal.normalize();
			
			if( normal.x * (triTranslated.p[0].x - camera.x) +
					normal.y * (triTranslated.p[0].y - camera.y) +
					normal.z * (triTranslated.p[0].z - camera.z) < 0) {
				
				// Illumination
				Vector lightDirection = new Vector(0,0,-1);
				lightDirection.normalize();
				
				// Shading
				double dp = normal.dot(lightDirection);
				// The dot product range is mapped onto the colour range to give
				// a shade.
				int c = (int) Math.abs(map(dp, 0, 1, 0, 255));
				triProjected.color = Color.rgb(c, c, c);
				
				// Project triangle from 3D to 2D space
				triProjected.p[0] = multiplyMatrixVector(triTranslated.p[0], matProj);
				triProjected.p[1] = multiplyMatrixVector(triTranslated.p[1], matProj);
				triProjected.p[2] = multiplyMatrixVector(triTranslated.p[2], matProj);
	
				// Scale into view
				triProjected.p[0].x += 1.0; triProjected.p[0].y += 1.0;
				triProjected.p[1].x += 1.0; triProjected.p[1].y += 1.0;
				triProjected.p[2].x += 1.0; triProjected.p[2].y += 1.0;
				
				triProjected.p[0].x *= 0.5 * (double) width;
				triProjected.p[0].y *= 0.5 * (double) height;
				triProjected.p[1].x *= 0.5 * (double) width;
				triProjected.p[1].y *= 0.5 * (double) height;
				triProjected.p[2].x *= 0.5 * (double) width;
				triProjected.p[2].y *= 0.5 * (double) height;
				
				// Store triangle for sorting
				trianglesToRaster.add(triProjected);
			}
		}// End for
		
		// Sort triangles from back to front
		trianglesToRaster.sort(null);
		// Draw them...
		for(int i=0; i<trianglesToRaster.size(); i++) {
			Triangle t = trianglesToRaster.get(i);
			fillTriangle(g, t, t.color);
		}
	}
/*	
	public void drawTriangle(GraphicsContext g, Triangle t, Color c) {
		g.setStroke(c);
		
		g.strokeLine(t.p[0].x, t.p[0].y, t.p[1].x, t.p[1].y);
		g.strokeLine(t.p[1].x, t.p[1].y, t.p[2].x, t.p[2].y);
		g.strokeLine(t.p[2].x, t.p[2].y, t.p[0].x, t.p[0].y);
	}
*/	
	
	public void fillTriangle(GraphicsContext g, Triangle t, Color c) {
		g.beginPath();
		g.moveTo(t.p[0].x, t.p[0].y);
		g.lineTo(t.p[1].x, t.p[1].y);
		g.lineTo(t.p[2].x, t.p[2].y);
		g.lineTo(t.p[0].x, t.p[0].y);
		g.closePath();
		g.setFill(c);
		g.fill();
	}

	public Vector multiplyMatrixVector(Vector i, double[][] m) {
		Vector o = new Vector(0,0,0);
		o.x = i.x * m[0][0] + i.y * m[1][0] + i.z * m[2][0]  + m[3][0];
		o.y = i.x * m[0][1] + i.y * m[1][1] + i.z * m[2][1]  + m[3][1];
		o.z = i.x * m[0][2] + i.y * m[1][2] + i.z * m[2][2]  + m[3][2];

		double w  = i.x * m[0][3] + i.y * m[1][3] + i.z * m[2][3] + m[3][3];
		
		if( w != 0 ) {
			o.x /= w; o.y /= w; o.z /= w;
		}
			
		return o;
	}

	public double[][] makeRotationZ(double angleRad) {
		double[][] matRotZ = new double[4][4];
		// Rotation Z
		matRotZ[0][0] = Math.cos(angleRad);
		matRotZ[0][1] = Math.sin(angleRad);
		matRotZ[1][0] = -Math.sin(angleRad);
		matRotZ[1][1] = Math.cos(angleRad);
		matRotZ[2][2] = 1;
		matRotZ[3][3] = 1;
		return matRotZ;
		
	}
	public double[][] makeRotationX(double angleRad) {
		double[][] matRotX = new double[4][4];
		// Rotation X
		matRotX[0][0] = 1;
		matRotX[1][1] = Math.cos(angleRad * 0.5);
		matRotX[1][2] = Math.sin(angleRad * 0.5);
		matRotX[2][1] = - Math.sin(angleRad * 0.5);
		matRotX[2][2] = Math.cos(angleRad * 0.5);
		matRotX[0][0] = 1;
		return matRotX;
	}
	public double[][] makeProjectionMatrix(double near, double far, double fov,
			double aspectRatio, double fovRad) {
		double[][] m = new double[4][4];
		m[0][0] = aspectRatio * fovRad;
		m[1][1] = fovRad;
		m[2][2] = far / (far - near);
		m[3][2] = (-far * near) / (far - near);
		m[2][3] = 1.0;
		m[3][3] = 0.0;
		return m;
	}

	/**
	 * Re-maps a number from one range to another. 
	 * 
	 * @param valueCoord1 The value to be converted
	 * @param startCoord1 Lower bound of the value's current range
	 * @param endCoord1 Upper bound of the value's current range
	 * @param startCoord2 Lower bound of the value's target range
	 * @param endCoord2 Upper bound of the value's target range
	 * @return The number re-mapped
	 */
	public static double map(double valueCoord1,
	        double startCoord1, double endCoord1,
	        double startCoord2, double endCoord2) {

	    if (Math.abs(endCoord1 - startCoord1) < EPSILON) {
	        throw new ArithmeticException("/ 0");
	    }

	    double offset = startCoord2;
	    double ratio = (endCoord2 - startCoord2) / (endCoord1 - startCoord1);
	    return ratio * (valueCoord1 - startCoord1) + offset;
	}
	/* Triangle holds 3 points that define a triangle. */
	class Triangle implements Comparable<Triangle> {
		public Vector[] p = new Vector[3];
		public Color color = Color.WHITE;
		
		public Triangle() {
			
		}
		
		public Triangle(double x1, double y1, double z1,
				double x2, double y2, double z2,
				double x3, double y3, double z3) {
			Vector v = new Vector(x1,y1,z1);
			p[0] = v;
			v = new Vector(x2,y2,z2);
			p[1] = v;
			v = new Vector(x3,y3,z3);
			p[2] = v;
		}
		
		public Triangle(Vector v1, Vector v2, Vector v3) {
			p[0] = v1;
			p[1] = v2;
			p[2] = v3;
		}
		
		public Triangle clone() {
			Triangle t = new Triangle(p[0].x, p[0].y, p[0].z,
					p[1].x, p[1].y, p[1].z,
					p[2].x, p[2].y, p[2].z);
			t.color = color;
			return t;
		}

		@Override
		public int compareTo(Triangle t) {
			double z1 = (p[0].z + p[1].z + p[2].z) / 3.0;
			double z2 = (t.p[0].z + t.p[1].z + t.p[2].z) / 3.0;
			if( z1 > z2 )
				return -1;
			else if( z1 == z2 )
				return 0;
			
			return 1;
		}
	}
	
	/* Mesh is a list of triangles */
	class Mesh {
		public ArrayList<Triangle> tris = new ArrayList<>();
		
		public boolean loadFromObjectFile(String filename) {
			boolean success = false;
			ArrayList<Vector> vectors = new ArrayList<>();
			
			try {
				InputStream in = getClass().getResourceAsStream(filename);
				if( in == null )
					return false;
				StreamTokenizer st = new StreamTokenizer(in);
				while( st.nextToken() != StreamTokenizer.TT_EOF) {
					switch( st.ttype ) {
					case StreamTokenizer.TT_WORD:
						// Vertices
						if( st.sval.equals("v")) {
							st.nextToken();
							double x = st.nval;
							st.nextToken();
							double y = st.nval;
							st.nextToken();
							double z = st.nval;
							Vector v = new Vector(x,y,z);
							vectors.add(v);
							//System.out.println(v);
						} /* Faces */ else if( st.sval.equals("f") ) {
							st.nextToken();
							int i1 = (int)st.nval;
							st.nextToken();
							int i2 = (int)st.nval;
							st.nextToken();
							int i3 = (int)st.nval;
							try {
								Vector v1 = vectors.get(i1-1).clone();
								Vector v2 = vectors.get(i2-1).clone();
								Vector v3 = vectors.get(i3-1).clone();
								Triangle t = new Triangle(v1, v2, v3);
								tris.add(t);
							}catch(ArrayIndexOutOfBoundsException e) {
								System.err.println(filename + " object file has corrupted face indices.");
								return false;
							}
						}
						break;
					}
				}
				System.out.println("Read "+vectors.size()+" vectors and "+tris.size()+" faces.");
			}catch(IOException ioe) {
				System.out.println("Error reading "+filename+": " + ioe.getMessage());
				return false;
			}
			return true;		
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		launch(args);
	}

}
