<?php


namespace AppBundle\Controller\API;


use FOS\RestBundle\Controller\FOSRestController;
use FOS\RestBundle\Controller\Annotations\Route;

class HumAidItemsController extends FOSRestController {

    /**
     * @Route("/humaid_items") // TODO: fix  app_api_clients_postclienthumaiditemdelivery     ANY        ANY      ANY    /clients/{clientID}/humaiditem_delivery/{itemID}
     * @return \Symfony\Component\HttpFoundation\Response
     */
    public function getHumaiditemsAction()
    {
        $res =  $this->getDoctrine()->getEntityManager()
            ->createQuery('SELECT i.id, i.name, i.category, i.limitDays FROM AppBundle\Entity\HumAidItem i ORDER BY i.name')
            ->getResult();

        $view = $this->view($res, 200);
        return $this->handleView($view);
    }

}