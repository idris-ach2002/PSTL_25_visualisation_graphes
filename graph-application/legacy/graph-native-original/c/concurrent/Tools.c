#include "Tools.h"

int incr_or_max(_Atomic int* n, int max) {
    int res = *n;
    while (res < max) {
        if (atomic_compare_exchange_weak(n, &res, res + 1)) {
            return res;  // retourne l'index valide
        }
    }
    return -1;  // limite atteinte
}