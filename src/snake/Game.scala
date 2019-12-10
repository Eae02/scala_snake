package snake

import java.awt.Color

import javax.swing.JOptionPane
import org.lwjgl.PointerBuffer
import org.lwjgl.opengl.GLUtil
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW._
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL30._
import org.lwjgl.opengl.GL11._
import org.lwjgl.system.MemoryUtil.NULL

object Game {
	private val ENABLE_V_SYNC = true
	
	val TILE_SIZE = 24
	val TILES_W = 75
	val TILES_H = 35
	val PIXEL_W = TILES_W * TILE_SIZE
	val PIXEL_H = TILES_H * TILE_SIZE
	
	val ENERGY_BAR_W = 300.0f
	val TOP_BAR_H = 60
	
	lazy val energyEmptyTex = Tex.fromFile("./res/energyEmpty.png")
	lazy val energyFullTex = Tex.fromFile("./res/energyFull.png")
	lazy val energyBarH = energyEmptyTex.height * ENERGY_BAR_W / energyEmptyTex.width
	
	def drawEnergyBar(drawList: DrawList, x: Float, y: Float, color: ColorF, size: Float) = {
		drawList.add(x, y, ENERGY_BAR_W, energyBarH, Some(energyEmptyTex), ColorF.WHITE)
		drawList.add(x, y, ENERGY_BAR_W * size, energyBarH, Some(energyFullTex),
			0, 0, energyEmptyTex.width * size, energyEmptyTex.height, color)
	}
	
	def main(args: Array[String]): Unit = {
		GLFWErrorCallback.createPrint(System.err).set()
		
		if (!glfwInit) {
			System.err.println("Error initializing GLFW")
			System.exit(1)
		}
		
		glfwDefaultWindowHints()
		glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
		
		//Creates the window
		val window = glfwCreateWindow(PIXEL_W, PIXEL_H + TOP_BAR_H, "Dream Snake", NULL, NULL)
		if (window == NULL) {
			val errorPointerBuffer = PointerBuffer.allocateDirect(1)
			glfwGetError(errorPointerBuffer)
			
			JOptionPane.showMessageDialog(null, errorPointerBuffer.getStringUTF8, "Error creating window", JOptionPane.ERROR_MESSAGE)
			System.exit(1)
		}
		
		val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor)
		glfwSetWindowPos(window, (videoMode.width - PIXEL_W) / 2, (videoMode.height - (PIXEL_H + TOP_BAR_H)) / 2)
		
		glfwMakeContextCurrent(window)
		glfwSwapInterval(if (ENABLE_V_SYNC) 1 else 0)
		
		glfwShowWindow(window)
		
		GL.createCapabilities()
		
		val debugProc = GLUtil.setupDebugMessageCallback
		
		val vao = glGenVertexArrays()
		glBindVertexArray(vao)
		
		glClearColor(0.1f, 0.2f, 0.4f, 1)
		
		val drawList = new DrawList
		val tex = Tex.fromFile("./res/test.png")
		
		val world = new World(TILES_W, TILES_H)
		val player1 = new Player(ColorF(1.0f, 0.2f, 0.2f))
		val player2 = new Player(ColorF(0.2f, 1.0f, 0.2f))
		
		val PLAYER_MARGIN_X = 20
		player1.pos = (PLAYER_MARGIN_X, TILES_H / 2)
		player2.pos = (TILES_W - PLAYER_MARGIN_X, TILES_H / 2)
		Player.moveSpeed = 5
		
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) => {
			if (action == GLFW_PRESS) {
				key match {
					case GLFW_KEY_ESCAPE => glfwSetWindowShouldClose(window, true)
					case GLFW_KEY_A      => player1.left
					case GLFW_KEY_D      => player1.right
					case GLFW_KEY_W      => player1.up
					case GLFW_KEY_S      => player1.down
					case GLFW_KEY_LEFT   => player2.left
					case GLFW_KEY_RIGHT  => player2.right
					case GLFW_KEY_UP     => player2.up
					case GLFW_KEY_DOWN   => player2.down
					case GLFW_KEY_LEFT_SHIFT => player1.boost = true
					case GLFW_KEY_RIGHT_SHIFT => player2.boost = true
					case _ => 
				}
			} else if (action == GLFW_RELEASE) {
				key match {
					case GLFW_KEY_LEFT_SHIFT => player1.boost = false
					case GLFW_KEY_RIGHT_SHIFT => player2.boost = false
					case _ =>
				}
			}
		})
		
		var gameOver = false
		
		var dt: Float = 0
		while (!glfwWindowShouldClose(window)) {
			val frameStartTime = System.nanoTime
			
			glClear(GL_COLOR_BUFFER_BIT)
			
			def checkHasLost(): Unit = {
				if (player1.intersectsTail(player1.pos) || player2.intersectsTail(player1.pos)) {
					System.out.println("Player 2 Wins!")
					gameOver = true
				}
				if (player2.intersectsTail(player2.pos) || player1.intersectsTail(player2.pos)) {
					System.out.println("Player 1 Wins!")
					gameOver = true
				}
			}
			
			if (!gameOver) {
				player1.update(dt)
				player2.update(dt)
				
				do {
					checkHasLost()
				} while (player1.move())
				
				do {
					checkHasLost()
				} while (player2.move())
				
				if (Player.moveSpeed < 15.0f) {
					Player.moveSpeed += dt * 0.3f
				}
				
				world.update(player1, player2, dt)
			}
			
			drawList.begin()
			
			world.draw(drawList)
			
			player1.draw(drawList)
			player2.draw(drawList)
			
			drawEnergyBar(drawList, 5, PIXEL_H + TOP_BAR_H - energyBarH - 5, player1.color, player1.energy)
			drawEnergyBar(drawList, PIXEL_W - ENERGY_BAR_W - 5, PIXEL_H + TOP_BAR_H - energyBarH - 5, player2.color, player2.energy)
			
			drawList.end(PIXEL_W, PIXEL_H + TOP_BAR_H)
			
			glFinish()
			
			glfwSwapBuffers(window)
			
			glfwPollEvents()
			
			dt = (System.nanoTime - frameStartTime) * 1E-9f
		}
		
		glfwFreeCallbacks(window)
		glfwDestroyWindow(window)
		
		glfwTerminate()
		glfwSetErrorCallback(null).free()
	}
}
