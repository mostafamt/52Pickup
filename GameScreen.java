import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import java.util.ArrayList;

public class GameScreen extends BaseScreen
{
    private BaseActor background;

    private ArrayList<Card> cardList;
    private ArrayList<Pile> pileList;

    private BaseActor glowEffect;
    private float hintTimer;

    // game world dimensions
    final int mapWidth = 800;
    final int mapHeight = 600;

    public GameScreen(BaseGame g)
    {
        super(g);
    }

    public void create() 
    {   
        background = new BaseActor();
        background.setTexture( new Texture(Gdx.files.internal("assets/felt.jpg")) );
        mainStage.addActor(background);

        pileList = new ArrayList<Pile>();
        Texture pileTex = new Texture(Gdx.files.internal("assets/cardBack.png"));
        for (int n = 0; n < 4; n++)
        {
            Pile pile = new Pile();
            pile.setTexture( pileTex );
            pile.setWidth(120);
            pile.setHeight(140);
            pile.setOriginCenter();
            pile.setPosition(70 + 180*n, 400);
            pile.setRectangleBoundary();
            pileList.add( pile );
            mainStage.addActor( pile );
        }

        String[] rankNames = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        String[] suitNames = {"Clubs", "Hearts", "Spades", "Diamonds"};

        cardList = new ArrayList<Card>();
        for (int r = 0; r < rankNames.length; r++)
        {
            for (int s = 0; s < suitNames.length; s++)
            {
                Card card = new Card( rankNames[r], suitNames[s] );
                String fileName = "assets/card" + suitNames[s] + rankNames[r] + ".png";
                card.setTexture( new Texture(Gdx.files.internal(fileName)) );
                card.setWidth(80);
                card.setHeight(100);
                card.setOriginCenter();
                card.setRectangleBoundary();

                cardList.add(card);

                card.addListener(
                    new InputListener() 
                    {
                        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) 
                        {  
                            if (!card.dragable)
                                return false;

                            card.setZIndex(1000); // render currently dragged card on top
                            card.offsetX = x;
                            card.offsetY = y;
                            card.originalX = event.getStageX();
                            card.originalY = event.getStageY();
                            return true; // T = call other methods
                        }

                        public void touchDragged(InputEvent event, float x, float y, int pointer) 
                        {
                            if (!card.dragable)
                                return;

                            card.moveBy(x - card.offsetX, y - card.offsetY);
                        }

                        public void touchUp(InputEvent event, float x, float y, int pointer, int button) 
                        {  
                            boolean overPile = false;
                            for (Pile pile : pileList)
                            {
                                if ( card.overlaps(pile, false) )
                                {
                                    overPile = true;
                                    if ( card.getRankIndex() == pile.getRankIndex() + 1 
                                    && card.getSuit().equals( pile.getSuit() ) )
                                    {
                                        float targetX = pile.getX() + pile.getOriginX() - card.getOriginX();
                                        float targetY = pile.getY() + pile.getOriginY() - card.getOriginY();
                                        card.dragable = false;
                                        card.addAction( Actions.moveTo( targetX, targetY, 0.5f ) );
                                        pile.addCard(card);
                                        return;
                                    }
                                }
                            }

                            if (overPile) // overlapping but not right one, move off the pile
                                card.addAction( Actions.moveTo( 
                                        card.originalX - card.offsetX, card.originalY - card.offsetY, 0.5f ) );

                            if ( card.getX() < 0 )
                                card.setX(0);
                            if ( card.getX() + card.getWidth() > mapWidth )
                                card.setX(mapWidth - card.getWidth());
                            if ( card.getY() < 0 )
                                card.setY(0);
                            if ( card.getY() + card.getHeight() > mapHeight )
                                card.setY(mapHeight - card.getHeight());
                        }
                    });

                mainStage.addActor(card);
                card.setZIndex(5); // cards created later should render earlier (on bottom)
            }
        }

        // move Aces to piles; randomize positions of all other cards
        for (Card card : cardList)
        {
            if ( card.getRank().equals("A")  )
            {
                for (Pile pile : pileList)
                {
                    if ( pile.isEmpty() )
                    {
                        card.moveToOrigin( pile );
                        pile.addCard( card );
                        card.dragable = false;
                        break;
                    }
                }
            }
            else
            {
                card.setPosition( MathUtils.random(720), MathUtils.random(200) );
            }
        }

        glowEffect = new BaseActor();
        Texture glowTex = new Texture(Gdx.files.internal("assets/glowBlue.png"));
        glowEffect.setTexture( glowTex );
        glowEffect.setWidth( cardList.get(0).getWidth() * 1.5f );
        glowEffect.setHeight( cardList.get(0).getHeight() * 1.5f );
        glowEffect.setOriginCenter();
        glowEffect.addAction(
            Actions.forever( Actions.sequence( Actions.fadeOut(0.5f), Actions.fadeIn(0.5f) ) ) );
        glowEffect.setVisible( false );
        mainStage.addActor( glowEffect );

        hintTimer = 0;
    }

    public void update(float dt) 
    {   
        hintTimer += dt;

        if ( Gdx.input.isTouched() )
        {
            hintTimer = 0;
            glowEffect.setVisible(false);
        }

        if ( hintTimer > 3 && !glowEffect.isVisible() )
        {
            for (Card hintCard : cardList)
            {
                if ( hintCard.dragable )
                {
                    glowEffect.setVisible(true);
                    glowEffect.moveToOrigin( hintCard );
                    glowEffect.toFront();
                    hintCard.toFront();
                    break; // exits for loop at first chance
                }
            }
        }
    }

    // InputProcessor methods for handling discrete input
    public boolean keyDown(int keycode)
    {
        if (keycode == Keys.P)    
            togglePaused();

        if (keycode == Keys.R)    
            game.setScreen( new GameScreen(game) );

        return false;
    }
}