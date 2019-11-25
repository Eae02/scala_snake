package snake

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._

case class ColorF(r: Float, g: Float, b: Float, a: Float = 1.0f) {
	def scaled(s: Float) = ColorF(r * s, g * s, b * s, a * s)
	
	def scaledRGB(s: Float) = ColorF(r * s, g * s, b * s, a)
	
	def scaledAlpha(s: Float) = ColorF(r, g, b, a * s)
}

object ColorF {
	val WHITE = ColorF(1, 1, 1)
}

case class Vertex(x: Float, y: Float, u: Float, v: Float, color: ColorF)

class DrawList {
	private val program = glCreateProgram()
	
	private val vertexShader = glCreateShader(GL_VERTEX_SHADER)
	glShaderSource(vertexShader, DrawList.vertexShaderCode)
	glCompileShader(vertexShader)
	glAttachShader(program, vertexShader)
	
	private val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
	glShaderSource(fragmentShader, DrawList.fragmentShaderCode)
	glCompileShader(fragmentShader)
	glAttachShader(program, fragmentShader)
	
	glLinkProgram(program)
	
	private val dispSizeUniformLocation = glGetUniformLocation(program, "uDispSize")
	private val texUniformLocation = glGetUniformLocation(program, "uTex")
	
	private val vertexBuffer = glGenBuffers()
	
	private var totalVertices = 0
	
	private class DrawGroup(val tex: Int) {
		var vertices = Vector.empty[Vertex]
	}
	
	private var drawGroups = Vector.empty[DrawGroup]
	
	private val blankTexture = createBlankTexture()
	
	private def createBlankTexture(): Int = {
		val tex = glGenTextures()
		glBindTexture(GL_TEXTURE_2D, tex)
		
		val textureData = BufferUtils.createByteBuffer(4)
		for (_ <- 0 until 4) {
			textureData.put(255.asInstanceOf[Byte])
		}
		textureData.flip()
		
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, textureData)
		
		tex
	}
	
	def begin(): Unit = {
		totalVertices = 0
		drawGroups = Vector.empty[DrawGroup]
	}
	
	private def initDrawGroup(texture: Option[Tex]): Unit = {
		val texGL: Int = texture.map(_.handle).getOrElse(blankTexture)
		if (!drawGroups.lastOption.exists(_.tex == texGL)) {
			drawGroups :+= new DrawGroup(texGL) 
		}
	}
	
	private def addVertex(vertex: Vertex): Unit = {
		drawGroups.last.vertices :+= vertex
		totalVertices += 1
	}
	
	def add(x: Float, y: Float, tex: Option[Tex], color: ColorF): Unit = {
		add(x, y, tex, 0, 0, tex.map(_.width).getOrElse[Int](1), tex.map(_.height).getOrElse[Int](1), color)
	}
	
	def add(x: Float, y: Float, w: Float, h: Float, tex: Option[Tex], color: ColorF): Unit = {
		add(x, y, w, h, tex, 0, 0, tex.map(_.width).getOrElse[Int](1), tex.map(_.height).getOrElse[Int](1), color)
	}
	
	def add(x: Float, y: Float, tex: Option[Tex], srcX: Float, srcY: Float, srcW: Float, srcH: Float, color: ColorF) {
		add(x, y, srcW, srcH, tex, srcX, srcY, srcW, srcH, color)
	}
	
	def add(x: Float, y: Float, w: Float, h: Float, tex: Option[Tex], srcX: Float, srcY: Float, srcW: Float, srcH: Float, color: ColorF) {
		initDrawGroup(tex)
		
		val texW = tex.map(_.width).getOrElse(1)
		val texH = tex.map(_.height).getOrElse(1)
		
		val minU = srcX / texW
		val minV = srcY / texH
		val maxU = (srcX + srcW) / texW
		val maxV = (srcY + srcH) / texH
		
		val v00 = Vertex(x,     y,     minU, minV, color)
		val v10 = Vertex(x + w, y,     maxU, minV, color)
		val v01 = Vertex(x,     y + h, minU, maxV, color)
		val v11 = Vertex(x + w, y + h, maxU, maxV, color)
		
		addVertex(v00)
		addVertex(v01)
		addVertex(v10)
		
		addVertex(v01)
		addVertex(v10)
		addVertex(v11)
	}
	
	def end(dispWidth: Int, dispHeight: Int): Unit = {
		glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer)
		
		val vertexUploadBuffer = BufferUtils.createFloatBuffer(totalVertices * 8)
		for (group <- drawGroups) {
			for (v <- group.vertices) {
				vertexUploadBuffer.put(v.x)
				vertexUploadBuffer.put(v.y)
				vertexUploadBuffer.put(v.u)
				vertexUploadBuffer.put(v.v)
				vertexUploadBuffer.put(v.color.r)
				vertexUploadBuffer.put(v.color.g)
				vertexUploadBuffer.put(v.color.b)
				vertexUploadBuffer.put(v.color.a)
			}
		}
		vertexUploadBuffer.flip()
		
		glBufferData(GL_ARRAY_BUFFER, vertexUploadBuffer, GL_STREAM_DRAW)
		
		glEnable(GL_BLEND)
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
		
		glUseProgram(program)
		glUniform1i(texUniformLocation, 0)
		glUniform2f(dispSizeUniformLocation, dispWidth / 2.0f, dispHeight / 2.0f)
		
		glEnableVertexAttribArray(0)
		glEnableVertexAttribArray(1)
		
		glVertexAttribPointer(0, 4, GL_FLOAT, false, 32, 0)
		glVertexAttribPointer(1, 4, GL_FLOAT, false, 32, 16)
		
		var vertexOffset = 0
		for (group <- drawGroups) {
			glBindTexture(GL_TEXTURE_2D, group.tex)
			
			glDrawArrays(GL_TRIANGLES, vertexOffset, group.vertices.size)
			vertexOffset += group.vertices.size
		}
		
		glDisableVertexAttribArray(0)
		glDisableVertexAttribArray(1)
		
		glDisable(GL_BLEND)
	}
}

object DrawList {
	private val vertexShaderCode = """#version 330 core
layout(location=0) in vec4 posAndTexCoord_in;
layout(location=1) in vec4 color_in;

uniform vec2 uDispSize;

out vec2 vTexCoord;
out vec4 vColor;

void main() {
	vec2 sPos = (posAndTexCoord_in.xy / uDispSize) - vec2(1.0);
	gl_Position = vec4(sPos, 0.0, 1.0);
	vTexCoord = posAndTexCoord_in.zw;
	vColor = color_in;
}
"""
	
	private val fragmentShaderCode = """#version 330 core
in vec2 vTexCoord;
in vec4 vColor;

layout(location=0) out vec4 color_out;

uniform sampler2D uTex;

void main()
{
	color_out = texture(uTex, vTexCoord) * vColor;
}
"""
}
