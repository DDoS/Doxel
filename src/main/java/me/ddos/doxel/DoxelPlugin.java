package me.ddos.doxel;

/**
 * An interface that must be implemented by the main class of plugins for Doxel.
 *
 * @author DDoS
 */
public interface DoxelPlugin {
	/**
	 * Called upon loading the plugin, right before generation of the model's mesh. This method can
	 * be used to accomplish task such as changing the noise source or the polygonizer.
	 */
	public void load();

	/**
	 * Called upon unloading of the plugin, right after the rendering cycle has been stopped, but
	 * before the Display is destroyed.
	 */
	public void unload();

	/**
	 * Called inside the render cycle, before the new render is created.
	 */
	public void preRender();

	/**
	 * Called inside the render cycle, after the new render is created and displayed.
	 */
	public void postRender();
}
