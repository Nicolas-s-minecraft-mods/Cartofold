# Minimap and map of the world

The maps show only the places that you explored. The server records the chunks near you as you move.

## Minimap

The minimap shows the map around you, the north, your arrow and your coordinates.

- Press **Y** to show or hide it.
- Press **[** to zoom out and **]** to zoom in. The minimap has five zoom levels.
- The minimap zooms out when you go faster, also in a vehicle. It has five levels of speed: stopped, walk, sprint, a little faster (a horse, a boat on water) and very fast (an elytra, a boat on ice). The zoom level is the zoom when you stop. The setting `speedZoom` stops this.

The [settings](settings.md) select the shape, the corner of the screen and the size. The minimap can also turn with you.

## Entities

The minimap and the map of the world show the loaded entities near you as dots: the players in white, the hostile mobs in red, the animals and the passive mobs in green, and the items and the vehicles in yellow. The [settings](settings.md#entities) select the kinds. A server can stop each kind (see the [settings of the server](settings.md#settings-of-the-server)).

## Map of the world

Press **M** to open the map of the world.

| Action | Control |
| --- | --- |
| Move the map | Drag with the mouse, or use the arrow keys |
| Zoom | Mouse wheel |
| Center the map on you | Space |
| Add a waypoint, or change a waypoint | Right click |
| Click a marker of a layer | Left click |
| Select the surface or the caves | Button at the top left, Page Up, Page Down, Home |
| Show or hide the layers | Button **Layers** |
| Open the settings | Button **Settings** |
| Close the map | Escape, or **M** |

The map follows you until you move it. It shows the coordinates under the mouse at the bottom left.

### Names of the markers

The map does not draw a name above another name. Zoom in to see the hidden names. A marker under the mouse always shows its name.

## Caves

You are in a cave when you are 5 blocks or more under the surface, in the dark. A cave view then shows the caves at your height, in a band of 16 blocks.

- The minimap shows your band. Its coordinates say "(caves)".
- The map of the world opens on your band.
- On the map of the world, **Page Up** and **Page Down** select the band above or below. **Home** selects the surface.

The setting **Caves** turns this view off. The dimensions with a ceiling, such as the Nether, have no cave view: their map shows the floor under the ceiling.

## Layers

Other mods add layers to the maps, for example territories, routes and markers. Use the button **Layers** of the map of the world to show or hide each layer. The button **HUD** of a layer shows or hides its markers in the [world](waypoints.md#markers-in-the-world).
