#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#include "../global.h"
#include "test_utils.h"

void reset_forces(double forces[][2], int n)
{
    for(int i=0;i<n;i++){
        forces[i][0] = 0.0;
        forces[i][1] = 0.0;
    }
}

void check_nan_positions()
{
    for(int i=0;i<num_nodes;i++){
        if(isnan(vertices[i].x) || isnan(vertices[i].y)){
            printf("FAIL: NaN position at node %d\n", i);
            exit(1);
        }

        if(isinf(vertices[i].x) || isinf(vertices[i].y)){
            printf("FAIL: INF position at node %d\n", i);
            exit(1);
        }
    }
}

void print_positions(int max)
{
    for(int i=0;i<num_nodes && i<max;i++){
        printf("node %d : %f %f\n", i, vertices[i].x, vertices[i].y);
    }
}