package catgirlroutes.utils.dungeon.tiles


interface Tile {
    val x: Int
    val z: Int
    var state: RoomState
}