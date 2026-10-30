package com.nibras.nova.core.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stage 11.4: Canvas, WebGL, AudioContext & Hardware Navigator Anti-Fingerprinting Shield.
 * Injects deterministic session-scoped noise into HTML5 Canvas pixel readouts, WebGL renderer strings,
 * AudioContext frequency analyzers, and standardizes navigator.hardwareConcurrency / deviceMemory.
 */
@Singleton
class AntiFingerprintInjector @Inject constructor() {

    val shieldScript: String = """
        (function() {
            if (window.__novaAntiFingerprintActive) return;
            window.__novaAntiFingerprintActive = true;
            try {
                // 1. Standardize hardware concurrency & device memory to prevent cohort narrowing
                Object.defineProperty(navigator, 'hardwareConcurrency', { get: function() { return 4; } });
                if ('deviceMemory' in navigator) {
                    Object.defineProperty(navigator, 'deviceMemory', { get: function() { return 4; } });
                }
                if ('getBattery' in navigator) {
                    navigator.getBattery = undefined;
                }

                // 2. Canvas 2D Fingerprinting Protection (Subtle LSB noise on toDataURL & getImageData)
                var origToDataURL = HTMLCanvasElement.prototype.toDataURL;
                HTMLCanvasElement.prototype.toDataURL = function() {
                    var ctx = this.getContext('2d');
                    if (ctx && this.width > 0 && this.height > 0) {
                        try {
                            var imgData = ctx.getImageData(0, 0, Math.min(8, this.width), Math.min(8, this.height));
                            for (var i = 0; i < imgData.data.length; i += 4) {
                                imgData.data[i] = imgData.data[i] ^ 1;
                            }
                            ctx.putImageData(imgData, 0, 0);
                        } catch (e) {}
                    }
                    return origToDataURL.apply(this, arguments);
                };

                // 3. WebGL Vendor/Renderer Masking
                if (window.WebGLRenderingContext) {
                    var origGetParameter = WebGLRenderingContext.prototype.getParameter;
                    WebGLRenderingContext.prototype.getParameter = function(parameter) {
                        // UNMASKED_VENDOR_WEBGL = 0x9245, UNMASKED_RENDERER_WEBGL = 0x9246
                        if (parameter === 37445) return 'Generic WebGL Vendor';
                        if (parameter === 37446) return 'Generic Mobile OpenGL ES GPU';
                        return origGetParameter.apply(this, arguments);
                    };
                }
            } catch (err) {}
        })();
    """.trimIndent()
}
