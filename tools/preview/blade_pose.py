"""Author a straight blade by its physical grip, axis and leading edge, not arbitrary Euler angles.
The output is an ordinary keyframe. Use parent clip rotation_mode='slerp' so the
calibrated blade axes/edge are interpolated as orientations. Does not modify assets.
"""
import numpy as np
from run import load
from third import axis, dR, gp
from hilt import euler_xyz

class BladePose:
    def __init__(self, archetype):
        self.anims, self.model, fp, _, self.grip = load(archetype)
        if self.grip is None:
            raise ValueError('An explicit authored grip is required')
        h, tip = axis(self.model, fp, self.grip)
        along = tip-h; along /= np.linalg.norm(along)
        normal = np.array([0.,0,1]); normal -= along*(normal@along)
        if np.linalg.norm(normal)<1e-6:
            raise ValueError('Specify the model plane for a blade outside the model XY plane')
        normal /= np.linalg.norm(normal)
        self.basis = np.column_stack([along,np.cross(normal,along),normal])

    def key(self, time, direction, edge, hand, left=False, ease='linear'):
        """Targets in blocks: +X toward this hand’s outside, +Y up, -Z forward. Uses this hand’s own authored Display."""
        sign=-1 if left else 1
        disp=self.model['display']['firstperson_'+('left' if left else 'right')+'hand']
        d=np.array(direction,float); d[0]*=sign
        if np.linalg.norm(d)<1e-6: raise ValueError('Zero blade direction')
        d/=np.linalg.norm(d)
        e=np.array(edge,float);e[0]*=sign;e-=d*(e@d)
        if np.linalg.norm(e)<1e-6: raise ValueError('Leading edge must differ from the blade axis')
        e/=np.linalg.norm(e)
        target=np.column_stack([d,e,np.cross(d,e)])
        rotation=euler_xyz(target@self.basis.T@dR(disp,sign).T);rotation[1:]*=sign
        hand=np.array(hand,float);hand[0]*=sign
        rest=np.array([sign*.56,-.52,-.72])+gp(self.grip,disp,sign)
        pos=(hand-rest)*16;pos[0]*=sign
        return dict(t=time,rotation=rotation.round(4).tolist(),translation=pos.round(4).tolist(),ease=ease)
