/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

package com.qualcomm.vuforia.samples.SampleApplication.utils;

public class LineShaders
{
    
    public static final String LINE_VERTEX_SHADER = " \n"
        + "attribute vec4 vertexPosition; \n"
        + "uniform mat4 modelViewProjectionMatrix; \n" + " \n"
        + "void main() \n" + "{ \n"
        + "   gl_Position = modelViewProjectionMatrix * vertexPosition; \n"
        + "} \n";
    
    public static final String LINE_FRAGMENT_SHADER = " \n" + " \n"
        + "precision mediump float; \n" + "uniform float opacity; \n"
        + "uniform vec3 color; \n" + " \n" + "void main() \n" + "{ \n"
        + "   gl_FragColor = vec4(color.r, color.g, color.b, opacity); \n"
        + "} \n";
    
}
