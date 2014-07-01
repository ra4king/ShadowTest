package com.ra4king.shadowtest;

import static org.lwjgl.opengl.ARBDepthClamp.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.util.HashMap;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.ra4king.opengl.util.GLProgram;
import com.ra4king.opengl.util.Mesh;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.loader.XMLMeshLoader;
import com.ra4king.opengl.util.math.Matrix3;
import com.ra4king.opengl.util.math.Matrix4;
import com.ra4king.opengl.util.math.MatrixStack;
import com.ra4king.opengl.util.math.Vector3;
import com.ra4king.opengl.util.math.Vector4;

/**
 * @author ra4king
 */
public class ShadowTest extends GLProgram {
	public static void main(String[] args) {
		new ShadowTest().run(3, 0);
	}
	
	private Shader shadowShader, shadowDummyShader;
	
	private Mesh cube, plane;
	
	private int shadowTexture;
	private int shadowFBO;
	
	private int shadowFBOwidth = 3200, shadowFBOheight = 2400;
	
	private float xRot, yRot;
	private float radius;
	
	private boolean showLightPerspective;
	
	public ShadowTest() {
		super("Shadow Test", 800, 600, true);
		
		Mouse.setGrabbed(true);
	}
	
	@Override
	public void init() {
		setFPS(0);
		
		glClearColor(0, 0, 0, 0);
		glClearDepth(1);
		
		glEnable(GL_DEPTH_TEST);
		glDepthRange(0, 1);
		glDepthMask(true);
		
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glFrontFace(GL_CW);
		
		glEnable(GL_DEPTH_CLAMP);
		
		HashMap<Integer,String> attribs = new HashMap<>();
		attribs.put(0, "position");
		attribs.put(1, "norm");
		
		shadowShader = new Shader(new ShaderProgram(readFromFile("shadow_test.vert"), readFromFile("shadow_test.frag"), attribs));
		shadowDummyShader = new Shader(new ShaderProgram(readFromFile("shadow_dummy.vert"), readFromFile("shadow_dummy.frag"), attribs));
		
		try {
			cube = XMLMeshLoader.createMesh(getClass().getResource("cube.xml"));
			plane = XMLMeshLoader.createMesh(getClass().getResource("plane.xml"));
		} catch(Exception exc) {
			exc.printStackTrace();
		}
		
		shadowTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, shadowTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, shadowFBOwidth, shadowFBOheight, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_BYTE, (ByteBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glBindTexture(GL_TEXTURE_2D, 0);
		
		shadowFBO = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, shadowFBO);
		
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowTexture, 0);
		
		int i = glCheckFramebufferStatus(GL_FRAMEBUFFER);
		if(i != GL_FRAMEBUFFER_COMPLETE)
			System.out.println("ERROR: " + i);
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}
	
	@Override
	public void resized() {}
	
	@Override
	public void update(long deltaTime) {
		xRot += Mouse.getDX() * 0.5f;
		yRot -= Mouse.getDY() * 0.5f;
		int dwheel = Mouse.getDWheel();
		if(dwheel > 0)
			radius += 0.5f;
		else if(dwheel < 0)
			radius -= 0.5f;
		
		yRot = Utils.clamp(yRot, -90, 90);
		radius = Utils.clamp(radius, -Float.MAX_VALUE, -4);
	}
	
	@Override
	public void keyPressed(int key, char c) {
		switch(key) {
			case Keyboard.KEY_SPACE:
				Mouse.setGrabbed(!Mouse.isGrabbed());
				break;
			case Keyboard.KEY_C:
				showLightPerspective = !showLightPerspective;
				break;
		}
	}
	
	@Override
	public void render() {
		final Vector4 lightDir = new Vector4(-1, -1, 0.5f, 0);
		
		final Matrix4 shadowViewMatrix = Utils.lookAt(new Vector3(lightDir.copy().mult(-5)), new Vector3(0, 0, 0), new Vector3(0, 1, 0));
		final Matrix4 shadowProjectionMatrix = new Matrix4().clearToOrtho(-5, 5, -4, 3, 1, 1000);
		
		{
			glBindFramebuffer(GL_FRAMEBUFFER, shadowFBO);
			glViewport(0, 0, shadowFBOwidth, shadowFBOheight);
			
			glClear(GL_DEPTH_BUFFER_BIT);
			
			shadowDummyShader.program.begin();
			
			glUniformMatrix4(shadowDummyShader.projectionMatrixUniform, false, shadowProjectionMatrix.toBuffer());
			glUniformMatrix4(shadowDummyShader.viewMatrixUniform, false, shadowViewMatrix.toBuffer());
			
			drawScene(shadowDummyShader);
			
			shadowDummyShader.program.end();
			
			glBindFramebuffer(GL_FRAMEBUFFER, 0);
		}
		
		{
			glViewport(0, 0, getWidth(), getHeight());
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			shadowShader.program.begin();
			
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, shadowTexture);
			
			glUniformMatrix4(shadowShader.shadowProjectionMatrixUniform, false, shadowProjectionMatrix.toBuffer());
			glUniformMatrix4(shadowShader.shadowViewMatrixUniform, false, shadowViewMatrix.toBuffer());
			
			final Matrix4 viewMatrix;
			
			if(showLightPerspective) {
				glUniformMatrix4(shadowShader.projectionMatrixUniform, false, shadowProjectionMatrix.toBuffer());
				
				viewMatrix = shadowViewMatrix;
			}
			else {
				glUniformMatrix4(shadowShader.projectionMatrixUniform, false, new Matrix4().clearToPerspectiveDeg(90, getWidth(), getHeight(), 1, 1000).toBuffer());
				
				viewMatrix = new Matrix4().clearToIdentity();
				viewMatrix.translate(0, 0, radius);
				viewMatrix.rotateDeg(yRot, 1, 0, 0);
				viewMatrix.rotateDeg(xRot, 0, 1, 0);
			}
			
			glUniformMatrix4(shadowShader.viewMatrixUniform, false, viewMatrix.toBuffer());
			glUniform4(shadowShader.lightPositionUniform, viewMatrix.mult(lightDir).toBuffer());
			
			drawScene(shadowShader);
			
			shadowShader.program.end();
			
			glBindTexture(GL_TEXTURE_2D, 0);
		}
	}
	
	private void drawScene(Shader program) {
		MatrixStack modelMatrix = new MatrixStack();
		
		Vector4 cubeColor = new Vector4(0.1f, 0.3f, 0.9f, 1);
		Vector4 cubeAmbient = cubeColor.copy().mult(0.1f).w(0);
		
		Vector4 planeColor = new Vector4(0.9f, 0.9f, 0, 1);
		Vector4 planeAmbient = planeColor.copy().mult(0.1f).w(0);
		
		{
			modelMatrix.pushMatrix();
			modelMatrix.getTop().translate(0, 0, -1);
			
			{
				modelMatrix.pushMatrix();
				
				if(program.diffuseUniform != -1) {
					glUniform4(program.diffuseUniform, cubeColor.toBuffer());
					glUniform4(program.ambientUniform, cubeAmbient.toBuffer());
					glUniformMatrix3(program.normalMatrixUniform, false, new Matrix3(modelMatrix.getTop()).inverse().transpose().toBuffer());
				}
				
				glUniformMatrix4(program.modelMatrixUniform, false, modelMatrix.getTop().toBuffer());
				cube.render();
				
				modelMatrix.popMatrix();
			}
			
			{
				modelMatrix.pushMatrix();
				
				modelMatrix.getTop().translate(2, 0, 2);
				modelMatrix.getTop().rotateDeg(45, 0, 1, 0);
				
				if(program.diffuseUniform != -1) {
					glUniform4(program.diffuseUniform, cubeColor.toBuffer());
					glUniform4(program.ambientUniform, cubeAmbient.toBuffer());
					glUniformMatrix3(program.normalMatrixUniform, false, new Matrix3(modelMatrix.getTop()).inverse().transpose().toBuffer());
				}
				
				glUniformMatrix4(program.modelMatrixUniform, false, modelMatrix.getTop().toBuffer());
				cube.render();
				
				modelMatrix.popMatrix();
			}
			
			{
				modelMatrix.pushMatrix();
				
				modelMatrix.getTop().translate(1, 2, 1);
				modelMatrix.getTop().rotateDeg(20, 0, 1, 0);
				
				if(program.diffuseUniform != -1) {
					glUniform4(program.diffuseUniform, cubeColor.toBuffer());
					glUniform4(program.ambientUniform, cubeAmbient.toBuffer());
					glUniformMatrix3(program.normalMatrixUniform, false, new Matrix3(modelMatrix.getTop()).inverse().transpose().toBuffer());
				}
				
				glUniformMatrix4(program.modelMatrixUniform, false, modelMatrix.getTop().toBuffer());
				cube.render();
				
				modelMatrix.popMatrix();
			}
			
			modelMatrix.popMatrix();
		}
		
		{
			modelMatrix.pushMatrix();
			
			modelMatrix.getTop().translate(0, -1.01f, 0);
			modelMatrix.getTop().scale(5);
			
			if(program.diffuseUniform != -1) {
				glUniform4(program.diffuseUniform, planeColor.toBuffer());
				glUniform4(program.ambientUniform, planeAmbient.toBuffer());
				glUniformMatrix3(program.normalMatrixUniform, false, new Matrix3(modelMatrix.getTop()).inverse().transpose().toBuffer());
			}
			
			glUniformMatrix4(program.modelMatrixUniform, false, modelMatrix.getTop().toBuffer());
			plane.render();
			
			modelMatrix.popMatrix();
		}
	}
	
	private static class Shader {
		private ShaderProgram program;
		private int lightPositionUniform;
		private int diffuseUniform;
		private int ambientUniform;
		
		private int normalMatrixUniform;
		
		private int modelMatrixUniform;
		private int viewMatrixUniform;
		private int projectionMatrixUniform;
		
		private int shadowViewMatrixUniform;
		private int shadowProjectionMatrixUniform;
		
		public Shader(ShaderProgram program) {
			this.program = program;
			
			lightPositionUniform = program.getUniformLocation("lightPosition");
			diffuseUniform = program.getUniformLocation("diffuse");
			ambientUniform = program.getUniformLocation("ambient");
			
			normalMatrixUniform = program.getUniformLocation("normalMatrix");
			
			modelMatrixUniform = program.getUniformLocation("modelMatrix");
			viewMatrixUniform = program.getUniformLocation("viewMatrix");
			projectionMatrixUniform = program.getUniformLocation("projectionMatrix");
			
			shadowViewMatrixUniform = program.getUniformLocation("shadowViewMatrix");
			shadowProjectionMatrixUniform = program.getUniformLocation("shadowProjectionMatrix");
			
			program.begin();
			glUniform1i(glGetUniformLocation(program.getProgram(), "shadowMap"), 0);
			program.end();
		}
	}
}
