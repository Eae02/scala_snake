package snake

class Battery {
	var pos: (Int, Int) = (0, 0)
	
	def draw(drawList: DrawList): Unit = {
		drawList.add(pos._1 * Game.TILE_SIZE, pos._2 * Game.TILE_SIZE, Game.TILE_SIZE, Game.TILE_SIZE, Some(Battery.tex), ColorF(1.0f, 0.9f, 0.2f))
	}
}

object Battery {
	private lazy val tex = Tex.fromFile("./res/battery.png")
}
