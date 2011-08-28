package it.sephiroth.android.library.imagezoom.easing;

public class Expo
{
	public static float easeOut(float elapsed, float duration)
	{
		// taken from clutter
		// return (t == d) ? 1.0 : -pow (2, -10 * t / d) + 1;
		return (float) ((elapsed >= duration) ? 1.0f : -Math.pow( 2, -10 * elapsed / duration ) + 1);
	}
}
