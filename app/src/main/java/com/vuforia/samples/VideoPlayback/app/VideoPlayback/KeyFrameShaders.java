/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.VideoPlayback.app.VideoPlayback;

public class KeyFrameShaders
{
    
    public static final String KEY_FRAME_VERTEX_SHADER = " \n"
        + "attribute vec4 vertexPosition; \n"
        + "attribute vec4 vertexNormal; \n"
        + "attribute vec2 vertexTexCoord; \n" 
        + "varying vec2 texCoord; \n"
        + "varying vec4 normal; \n"
        + "uniform mat4 modelViewProjectionMatrix; \n" 
        + "\n"
        + "void main() \n" 
        + "{ \n"
        + "   gl_Position = modelViewProjectionMatrix * vertexPosition; \n"
        + "   normal = vertexNormal; \n" 
        + "   texCoord = vertexTexCoord; \n"
        + "} \n";
    
    public static final String KEY_FRAME_FRAGMENT_SHADER = " \n" 
        + "\n"
        + "precision mediump float; \n" 
        + "varying vec2 texCoord; \n"
        + "uniform sampler2D texSampler2D; \n" 
        + " \n" 
        + "void main() \n"
        + "{ \n" 
        + "   gl_FragColor = texture2D(texSampler2D, texCoord); \n"
        + "} \n";
    
}
