package ie.engine.maths;
import java.util.List;
import ie.engine.maths.Koleada.Collision;
import ie.engine.objects.*;
import processing.core.PApplet;
import java.util.concurrent.*;

public class Physics implements Runnable{

    List<Entity> listObjs;

    public Semaphore entityLock;
    int frameCount = -1;
    int tick = 0;
    int gameTick = 0;
    PApplet pa;
    // engine constructor - takes the collision object
    public Physics(List<Entity> listObjs, PApplet pa, Koleada koleada){
        this.pa = pa;
        this.listObjs = listObjs;
        tick = 0;
        this.koleada = koleada;
        entityLock = koleada.entityLock;
    }
    public Coordinate gravity = new Coordinate((float) 0, (float) 9.8);
    public Koleada koleada;
    public Coordinate deeceleration = new Coordinate((float) 0.01, (float) 0.01);
    
    public void run(){
        // checks if the collision queue is empty (collisions can show up the queue during this code execution)
        while(!koleada.collisionQueue.isEmpty()){
            // pull the top item from the queue
            Collision collision = koleada.collisionQueue.poll();
            // unsure why but some collisions return null - will need to figure out
            // TODO FIX
            if(collision == null){

            } else{
                // makes sure that the object is marked as handled - this is important as both objects are colliding - therefore we
                // dont want both objects to double up collision calculations
                collision.from.handled = true;
                collision.to.handled = true;
                // calculates the resultant velocity on x and y
                collision.from.velocity.x = (collision.from.mass * collision.from.velocity.x + collision.to.mass * collision.to.velocity.x)/collision.to.mass + collision.from.mass;
                collision.from.velocity.y = (collision.from.mass * collision.from.velocity.y + collision.to.mass * collision.to.velocity.y)/collision.to.mass + collision.from.mass;
                // makes sure the object being hit is woken by collision engine
                collision.to.collision_sleeping = false;
                collision.to.velocity.x = 1.1f * (collision.to.mass * collision.to.velocity.x + collision.from.mass * collision.from.velocity.x)/collision.to.mass + collision.from.mass;
                collision.to.velocity.y = 1.1f * (collision.to.mass * collision.to.velocity.y + collision.from.mass * collision.from.velocity.y)/collision.to.mass + collision.from.mass;
                // flattens out acceleration of object just incase
                collision.to.acceleration.clear();
                collision.from.acceleration.clear();
            }
        }
        try{
            // checks if we can proceed with calculations for thread synchronization
            entityLock.acquire();
        
        for(Entity obj : listObjs){
            // checks if the object is sleeping
              
            obj.updateModel();
            obj.handled = false;
            if(obj.acceleration.hasMagnitude()){
                // checks if acceleration is      
                if(Math.abs(obj.acceleration.x) <= 0.01 )
                    obj.acceleration.x = 0;
                if(Math.abs(obj.acceleration.y) <= 0.01)
                    obj.acceleration.y = 0;

                obj.collision_sleeping = false;
                obj.velocity.add(obj.acceleration.x/60, obj.acceleration.y/60);
            }
            if(obj.velocity.hasMagnitude() && !(obj instanceof Projectile)){
                    if(Math.abs(obj.velocity.x) <= 0.01 )
                        obj.velocity.x = 0;
                    if(Math.abs(obj.velocity.y) <= 0.01)
                        obj.velocity.y = 0;
                    if(obj.velocity.x > 0){
                        //decellerate  c
                        obj.velocity.x -= deeceleration.x;
                    }
                    else if(obj.velocity.x < 0){
                        obj.velocity.x += deeceleration.x;
                    } 
                    if(obj.velocity.y > 0){
                        obj.velocity.y -= deeceleration.y;
                    } else if(obj.velocity.y < 0){
                        obj.velocity.y += deeceleration.y;
                    }
                    obj.collision_sleeping = false;
                
            } else {
                obj.collision_sleeping = true;
            }
            if(obj.isTouchingWall()){

                
                if((obj.getX() <= 0 ) ){
                    obj.setX(0);

                    obj.velocity.multiply(-0.4f, 0.4f);
                } else if((obj.getX() >= pa.width )){
                    obj.setX(pa.width);
                    obj.velocity.multiply(-0.4f, 0.4f);
                }
                if(obj.getY() <= 0 ){
                    obj.setY(0);
                    obj.velocity.multiply(0.4f, -0.4f);
                }
                else if(obj.getY() >= pa.height){
                    obj.setY(pa.height);
                    obj.velocity.multiply(0.4f, -0.4f);
                }
            }
            obj.velocity.add(obj.acceleration);
            if(obj instanceof Player){
                if(obj.velocity.x > 5)
                    obj.velocity.x = 5;
                else if(obj.velocity.x < -5)
                    obj.velocity.x = -5;
                
                if(obj.velocity.y > 5)
                    obj.velocity.y = 5;
                else if(obj.velocity.y < -5)
                    obj.velocity.y = -5;
                
            }
            obj.moveCoord(obj.velocity);
        }
        } 
        catch(InterruptedException exc){
            System.err.println(exc);
            Thread.currentThread().interrupt();

        }
        entityLock.release();
    }
}
