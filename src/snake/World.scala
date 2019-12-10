package snake

import java.awt.Color

class World(val width: Int, val height: Int) {
	private val gridCellPhase = new Array[Float](width * height)
	
	import World._
	
	(() => {
		val rand = new java.util.Random()
		for (i <- gridCellPhase.indices) {
			gridCellPhase(i) = rand.nextFloat() * Math.PI.toFloat * 2.0f;
		}
	})()
	
	private val batteries = new Array[Battery](3) 
	
	private val rand = new java.util.Random()
	
	private var swapBlockSpawnDelay = 5.0f
	private var swapBlockPos = (-1, -1)
	
	def update(player1: Player, player2: Player, dt: Float): Unit = {
		for (i <- batteries.indices) {
			if (batteries(i) != null && player1.pos == batteries(i).pos) {
				batteries(i) = null
				if (player1.energy == 1)
					player1.tailLen += 2
				else
					player1.energy = 1
			}
			if (batteries(i) != null && player2.pos == batteries(i).pos) {
				batteries(i) = null
				if (player2.energy == 1)
					player2.tailLen += 2
				else
					player2.energy = 1
			}
			
			if (batteries(i) == null) {
				batteries(i) = new Battery()
				do {
					batteries(i).pos = (rand.nextInt(width), rand.nextInt(height))
				} while (batteries(i).pos == player1.pos || batteries(i).pos == player2.pos ||
					player1.intersectsTail(batteries(i).pos) || player2.intersectsTail(batteries(i).pos))
			}
		}
		
		val SWAP_TIME = 10
		
		if (swapBlockPos._1 == -1) {
			if (swapBlockSpawnDelay > 0) {
				swapBlockSpawnDelay -= dt
			} else {
				swapBlockPos = (rand.nextInt(width), rand.nextInt(height))
			}
		} else if (swapBlockPos == player1.pos) {
			player2.swappedTime = SWAP_TIME
			swapBlockPos = (-1, -1)
			swapBlockSpawnDelay = 15
		} else if (swapBlockPos == player2.pos) {
			player1.swappedTime = SWAP_TIME
			swapBlockPos = (-1, -1)
			swapBlockSpawnDelay = 15
		}
	}
	
	def draw(drawList: DrawList): Unit = {
		val MIN_SHADE = 0.8f
		val MAX_SHADE = 1.0f
		
		val t = System.nanoTime * 1E-9f * 3
		
		for (y <- 0 until height) {
			for (x <- 0 until width) {
				val p = t + gridCellPhase(y * width + x)
				
				val s01 = Math.sin(p).toFloat * 0.5f + 0.5f
				val s = s01 * (MAX_SHADE - MIN_SHADE) + MIN_SHADE
				
				drawList.add(x * Game.TILE_SIZE, y * Game.TILE_SIZE, Game.TILE_SIZE, Game.TILE_SIZE,
					Some(gridTexture), ColorF(GRID_R * s, GRID_G * s, GRID_B * s))
			}
		}
		
		if (swapBlockPos._1 != -1) {
			drawList.add(swapBlockPos._1 * Game.TILE_SIZE, swapBlockPos._2 * Game.TILE_SIZE, Game.TILE_SIZE, Game.TILE_SIZE, Some(swapTexture), ColorF(1.0f, 1.0f, 1.0f))
		}
		
		for (battery <- batteries) {
			battery.draw(drawList)
		}
	}
}

object World {
	private lazy val gridTexture = Tex.fromFile("./res/cell.png")
	private lazy val swapTexture = Tex.fromFile("./res/swap.png")
	private val GRID_R = 0.5f
	private val GRID_G = 0.7f
	private val GRID_B = 1.0f
}
