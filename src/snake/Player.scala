package snake

import scala.collection.mutable

class Player(val color: ColorF) {
	var pos: (Int, Int) = (0, 0)
	
	private var direction: (Int, Int) = (0, 1)
	
	private val tail = mutable.Queue.empty[(Int, Int)]
	var tailLen = 8
	
	private var growDelay: Float = Player.GROW_TIME
	
	private var timeBeforeMove = 0.0f
	
	var boost = false
	
	var energy = 1.0f
	var energyRegenDelay = 0.0f
	
	var swappedTime = 0.0f
	def swapped = swappedTime > 0
	
	def left = {
		if (direction._1 == 0) {
			direction = if (swapped) (1, 0) else (-1, 0)
			timeBeforeMove = 0
		}
	}
	def right = {
		if (direction._1 == 0) {
			direction = if (swapped) (-1, 0) else (1, 0)
			timeBeforeMove = 0
		}
	}
	def up = {
		if (direction._2 == 0) {
			direction = if (swapped) (0, -1) else (0, 1)
			timeBeforeMove = 0
		}
	}
	def down = {
		if (direction._2 == 0) {
			direction = if (swapped) (0, 1) else (0, -1)
			timeBeforeMove = 0
		}
	}
	
	def intersectsTail(pos: (Int, Int)) = {
		tail.contains(pos)
	}
	
	def update(dt: Float): Unit = {
		timeBeforeMove -= dt
		
		swappedTime -= dt
		
		if (boost && energy > 0) {
			timeBeforeMove -= dt * 3
			energy -= dt
			energyRegenDelay = 0.0f
		}
		
		growDelay -= dt
		if (growDelay < 0) {
			tailLen += 1
			growDelay = Player.GROW_TIME
		}
		
		while (timeBeforeMove < 0) {
			tail.enqueue((pos._1, pos._2))
			while (tail.size > tailLen) {
				tail.dequeue()
			}
			
			def wrapMod(x: Int, m: Int): Int = ((x % m) + m) % m
			
			pos = (wrapMod(pos._1 + direction._1, Game.TILES_W), wrapMod(pos._2 + direction._2, Game.TILES_H))
			
			timeBeforeMove += Player.timeBetweenMoves
		}
	}
	
	def draw(drawList: DrawList): Unit = {
		val moveP = Math.min((1.0f - (timeBeforeMove / Player.timeBetweenMoves)) * 2.0f, 1.0f)
		
		drawList.add(pos._1 * Game.TILE_SIZE, pos._2 * Game.TILE_SIZE, Game.TILE_SIZE, Game.TILE_SIZE, Some(Player.tex), color.scaledAlpha(moveP))
		
		for (i <- tail.indices) {
			drawList.add(tail(i)._1 * Game.TILE_SIZE, tail(i)._2 * Game.TILE_SIZE, Game.TILE_SIZE, Game.TILE_SIZE, Some(Player.tex), color)
		}
	}
}

object Player {
	var moveSpeed = 5.0f
	
	private val GROW_TIME = 2
	
	def timeBetweenMoves = 1.0f / moveSpeed
	
	private lazy val tex = Tex.fromFile("./res/snakeCell.png")
}
