package top.fpsmaster.utils.math.anim;

import top.fpsmaster.utils.math.animation.Type;

public enum Easings implements Easing {
    LINEAR {
        @Override
        public double ease(double t) {
            return t;
        }
    },
    QUAD_IN {
        @Override
        public double ease(double t) {
            return t * t;
        }
    },
    QUAD_OUT {
        @Override
        public double ease(double t) {
            return t * (2 - t);
        }
    },
    QUAD_IN_OUT {
        @Override
        public double ease(double t) {
            return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
        }
    },
    CUBIC_IN {
        @Override
        public double ease(double t) {
            return t * t * t;
        }
    },
    CUBIC_OUT {
        @Override
        public double ease(double t) {
            double u = t - 1;
            return u * u * u + 1;
        }
    },
    CUBIC_IN_OUT {
        @Override
        public double ease(double t) {
            return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
        }
    },
    QUART_IN {
        @Override
        public double ease(double t) {
            return t * t * t * t;
        }
    },
    QUART_OUT {
        @Override
        public double ease(double t) {
            return 1 - Math.pow(1 - t, 4);
        }
    },
    QUART_IN_OUT {
        @Override
        public double ease(double t) {
            return t < 0.5 ? 8 * Math.pow(t, 4) : 1 - Math.pow(-2 * t + 2, 4) / 2;
        }
    },
    QUINT_IN {
        @Override
        public double ease(double t) {
            return t * t * t * t * t;
        }
    },
    QUINT_OUT {
        @Override
        public double ease(double t) {
            return 1 - Math.pow(1 - t, 5);
        }
    },
    QUINT_IN_OUT {
        @Override
        public double ease(double t) {
            return t < 0.5 ? 16 * Math.pow(t, 5) : 1 - Math.pow(-2 * t + 2, 5) / 2;
        }
    },
    SINE_IN {
        @Override
        public double ease(double t) {
            return 1 - Math.cos((t * Math.PI) / 2);
        }
    },
    SINE_OUT {
        @Override
        public double ease(double t) {
            return Math.sin((t * Math.PI) / 2);
        }
    },
    SINE_IN_OUT {
        @Override
        public double ease(double t) {
            return -(Math.cos(Math.PI * t) - 1) / 2;
        }
    },
    EXPO_IN {
        @Override
        public double ease(double t) {
            return t == 0 ? 0 : Math.pow(2, 10 * t - 10);
        }
    },
    EXPO_OUT {
        @Override
        public double ease(double t) {
            return t == 1 ? 1 : 1 - Math.pow(2, -10 * t);
        }
    },
    EXPO_IN_OUT {
        @Override
        public double ease(double t) {
            if (t == 0) return 0;
            if (t == 1) return 1;
            return t < 0.5 ? Math.pow(2, 20 * t - 10) / 2 : (2 - Math.pow(2, -20 * t + 10)) / 2;
        }
    },
    CIRC_IN {
        @Override
        public double ease(double t) {
            return 1 - Math.sqrt(1 - t * t);
        }
    },
    CIRC_OUT {
        @Override
        public double ease(double t) {
            return Math.sqrt(1 - Math.pow(t - 1, 2));
        }
    },
    CIRC_IN_OUT {
        @Override
        public double ease(double t) {
            return t < 0.5
                    ? (1 - Math.sqrt(1 - Math.pow(2 * t, 2))) / 2
                    : (Math.sqrt(1 - Math.pow(-2 * t + 2, 2)) + 1) / 2;
        }
    },
    BACK_IN {
        @Override
        public double ease(double t) {
            double c1 = 1.70158;
            double c3 = c1 + 1;
            return c3 * t * t * t - c1 * t * t;
        }
    },
    BACK_OUT {
        @Override
        public double ease(double t) {
            double c1 = 1.70158;
            double c3 = c1 + 1;
            double u = t - 1;
            return 1 + c3 * u * u * u + c1 * u * u;
        }
    },
    BACK_IN_OUT {
        @Override
        public double ease(double t) {
            double c1 = 1.70158;
            double c2 = c1 * 1.525;
            return t < 0.5
                    ? (Math.pow(2 * t, 2) * ((c2 + 1) * 2 * t - c2)) / 2
                    : (Math.pow(2 * t - 2, 2) * ((c2 + 1) * (t * 2 - 2) + c2) + 2) / 2;
        }
    },
    ELASTIC_IN {
        @Override
        public double ease(double t) {
            if (t == 0 || t == 1) return t;
            double c4 = (2 * Math.PI) / 3;
            return -Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * c4);
        }
    },
    ELASTIC_OUT {
        @Override
        public double ease(double t) {
            if (t == 0 || t == 1) return t;
            double c4 = (2 * Math.PI) / 3;
            return Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1;
        }
    },
    ELASTIC_IN_OUT {
        @Override
        public double ease(double t) {
            if (t == 0 || t == 1) return t;
            double c5 = (2 * Math.PI) / 4.5;
            return t < 0.5
                    ? -(Math.pow(2, 20 * t - 10) * Math.sin((20 * t - 11.125) * c5)) / 2
                    : (Math.pow(2, -20 * t + 10) * Math.sin((20 * t - 11.125) * c5)) / 2 + 1;
        }
    },
    BOUNCE_IN {
        @Override
        public double ease(double t) {
            return 1 - BOUNCE_OUT.ease(1 - t);
        }
    },
    BOUNCE_OUT {
        @Override
        public double ease(double t) {
            double n1 = 7.5625;
            double d1 = 2.75;
            if (t < 1 / d1) {
                return n1 * t * t;
            } else if (t < 2 / d1) {
                double u = t - 1.5 / d1;
                return n1 * u * u + 0.75;
            } else if (t < 2.5 / d1) {
                double u = t - 2.25 / d1;
                return n1 * u * u + 0.9375;
            } else {
                double u = t - 2.625 / d1;
                return n1 * u * u + 0.984375;
            }
        }
    },
    BOUNCE_IN_OUT {
        @Override
        public double ease(double t) {
            return t < 0.5
                    ? (1 - BOUNCE_OUT.ease(1 - 2 * t)) / 2
                    : (1 + BOUNCE_OUT.ease(2 * t - 1)) / 2;
        }
    };

    public static Easing fromLegacy(Type type) {
        if (type == null) return LINEAR;
        switch (type) {
            case LINEAR:
                return LINEAR;
            case EASE_IN_QUAD:
                return QUAD_IN;
            case EASE_OUT_QUAD:
                return QUAD_OUT;
            case EASE_IN_OUT_QUAD:
                return QUAD_IN_OUT;
            case EASE_IN_ELASTIC:
                return ELASTIC_IN;
            case EASE_OUT_ELASTIC:
                return ELASTIC_OUT;
            case EASE_IN_OUT_ELASTIC:
                return ELASTIC_IN_OUT;
            case EASE_IN_BACK:
                return BACK_IN;
            case EASE_OUT_BACK:
                return BACK_OUT;
            case EASE_OUT_QUINT:
                return QUINT_OUT;
            default:
                return LINEAR;
        }
    }
}
