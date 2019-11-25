package snake

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.stb.STBImage._
import org.lwjgl.system.MemoryStack.stackPush

class Tex(val width: Int, val height: Int) {
	val handle: Int = glGenTextures()
}

object Tex {
	stbi_set_flip_vertically_on_load(true)
	
	def fromFile(name: String): Tex = {
		val stackFrame = stackPush()
		try {
			val widthBuf= stackFrame.mallocInt(1)
			val heightBuf= stackFrame.mallocInt(1)
			val componentsBuf= stackFrame.mallocInt(1)
			
			val imageData = stbi_load(name, widthBuf, heightBuf, componentsBuf, 4)
			if (imageData == null) {
				System.err.println("Error loading texture from " + name + ": " + stbi_failure_reason)
				System.exit(1)
			}
			
			widthBuf.rewind
			heightBuf.rewind
			
			try {
				val tex = new Tex(widthBuf.get, heightBuf.get)
				
				glBindTexture(GL_TEXTURE_2D, tex.handle)
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, tex.width, tex.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData)
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0)
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, 0)
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0)
				
				tex
			} finally {
				stbi_image_free(imageData)
			}
		} finally {
			stackFrame.pop()
		}
	}
}
