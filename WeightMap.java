import java.io.File;
import java.io.IOException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import javax.imageio.ImageIO;

public class WeightMap {
	public void createWeightMap(String inputPath, double weightC, double weightS, double weightE) {
		BufferedImage image = null;
		try {
			image = ImageIO.read(new File(inputPath));
			System.out.println("Reading complete");
		}catch(IOException e) {
			System.out.println("File Reading Error: "+e);
		}
		
		int width = image.getWidth();
		int height = image.getHeight();
		
		double[][] Contrast = measureContrast(image);
		
		double[][] Saturation = measureSaturation(image);
		
		double[][] Wellexposedness = measureWellexposedness(image);
		
		double[][] result = CalculateWeightMap(Contrast, Saturation, Wellexposedness, width, height, weightC, weightS, weightE);
		
		BufferedImage weightMap = produceWeightMap(result, width, height);
		//write image
		try {
			ImageIO.write(weightMap, "png", new File("weight"+inputPath));
			System.out.println("weight"+inputPath+" Printing complete");
		}catch(IOException e) {
			System.out.println("File Printing Error: "+e);
		}
		
	}
	
	private double returnColorValue(double[][] grayScale, int width, int height, int x, int y) {
		if(x<0)
			x=x+1;
		if(y<0)
			y=y+1;
		if(x>width-1)
			x=x-1;
		if(y>height-1)
			y=y-1;
		return grayScale[y][x];
	}
	
	private double[][] measureContrast(BufferedImage image) {
		double[][] grayScale = createGrayscaleImage(image);
		//WritableRaster raster = grayScale.getRaster();
		int width = image.getWidth();
		int height = image.getHeight();
		double copy[][] = new double[height][width];
		double sum=0;
		 //3*3 Laplacian filter (0, 1, 0), (1, -4, 1), (0, 1, 0)
		for(int y=0;y<height;y++)
			for(int x=0;x<width;x++) {
				sum = (0*(returnColorValue(grayScale, width, height, x-1,y-1))) + (1*(returnColorValue(grayScale, width, height, x, y-1))) + (0*(returnColorValue(grayScale, width, height, x+1, y-1)))
						+ (1*(returnColorValue(grayScale, width, height, x-1, y))) + (-4*(returnColorValue(grayScale, width, height, x,y))) + (1*(returnColorValue(grayScale, width, height, x+1, y))) +
						(0*(returnColorValue(grayScale, width, height, x-1, y+1))) + (1*(returnColorValue(grayScale, width, height, x, y+1))) + (0*(returnColorValue(grayScale, width, height, x+1, y+1)));				
				copy[y][x] = sum;
			}
		return copy;
		
	}
	
	private BufferedImage copyImage(BufferedImage bi) {
		 ColorModel cm = bi.getColorModel();
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = bi.copyData(null);
		 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
	
	private double[][] createGrayscaleImage(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		double[][] copy = new double[height][width];
		for(int y=0;y<height;y++)
			for(int x=0;x<width;x++) {
				Color c = new Color(image.getRGB(x, y) & 0x00ffffff);
				copy[y][x] = ((0.2989 * c.getRed()+0.5870 * c.getGreen()+0.1140 * c.getBlue())/255);
			}
		return copy;
	}
	private double[][] measureSaturation(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		double copy[][] = new double[height][width];
		
		int p=0;
		double r=0, g=0, b=0, avg=0, sd=0;
		for(int y=0;y<height;y++)
			for(int x=0;x<width;x++) {
				p=image.getRGB(x, y);
				r=((p>>16)&0xff)/(double)255;
				g=((p>>8)&0xff)/(double)255;
				b=(p&0xff)/(double)255;
				//System.out.println(r);
				avg = (r+g+b)/3;
				sd = Math.sqrt(((r-avg)*(r-avg)+(g-avg)*(g-avg)+(b-avg)*(b-avg))/3);
				//System.out.println(sd);
				copy[y][x]=sd;
			}
		return copy;
	}
	
	private double[][] measureWellexposedness(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		double copy[][] = new double[height][width];
		//BufferedImage copy = copyImage(image);
		int p=0;
		double r=0, g=0, b=0;
		for(int y=0;y<height;y++)
			for(int x=0;x<width;x++) {
				p=image.getRGB(x, y);
				r=(((p>>16)&0xff)/(double)255);
				g=(((p>>8)&0xff)/(double)255);
				b=((p&0xff)/(double)255);
				r = Math.exp(-0.5*(r-0.5)*(r-0.5)*25);
				//System.out.println(r);
				g = Math.exp(-0.5*(g-0.5)*(g-0.5)*25);
				b = Math.exp(-0.5*(b-0.5)*(b-0.5)*25);
				//System.out.println(r);	//0???
				copy[y][x]=r*g*b;
				//copy.setRGB(x,y,(int) (r*g*b));
			}
		return copy;
	}
	
	private double[][] CalculateWeightMap(double[][] C, double[][] S, double[][] E, int width, int height, double weightC, double weightS, double weightE) {
		double copy[][] = new double[height][width];
		double Wij;
		for(int y=0;y<height;y++)
			for(int x=0;x<width;x++) {
				Wij = Math.pow(C[y][x], weightC) * Math.pow(S[y][x], weightS) * Math.pow(E[y][x], weightE);
				if(x==5&&y==5) {
					System.out.println(C[y][x]);
					System.out.println(S[y][x]);
					System.out.println(E[y][x]);
					System.out.println(Wij);
				}
				copy[y][x] = Wij;
			}
		return copy;
	}
	
	private BufferedImage produceWeightMap(double[][] result, int width, int height) {
		BufferedImage weightMap = new BufferedImage(width,height, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = weightMap.getRaster();
		int[] temp = new int[1];
		for(int y=0;y<height;y++)
			for(int x=0;x<width;x++) {
				temp[0] = (int) (result[y][x]*255);
				raster.setPixel(x,y,temp);
			}
		return weightMap;
	}
}

