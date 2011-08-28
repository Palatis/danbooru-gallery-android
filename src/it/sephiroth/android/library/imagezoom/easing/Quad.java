package it.sephiroth.android.library.imagezoom.easing;

public class Quad
{
	public static float easeOut(float elapsedMs, float durationMs)
	{
		float progress = elapsedMs / durationMs;

		return -1.0f * progress * (progress - 2.0f);
	}
}
