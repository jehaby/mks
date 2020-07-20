<?php

namespace AppBundle\Controller\API;

use AppBundle\Entity\Client;
use AppBundle\Entity\HumAidItem;
use AppBundle\Entity\HumAidItemDelivery;
use FOS\RestBundle\Controller\FOSRestController;
use FOS\RestBundle\Controller\Annotations\View;
use FOS\RestBundle\Controller\Annotations\Route;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;


class ClientsController extends FOSRestController {

    /**
     * @return \Symfony\Component\HttpFoundation\Response
     * @View(serializerGroups={"client"})
     */
    public function getClientsSearchAction()
    {
        // TODO: reuse https://github.com/homelessru/mks/blob/master/shared/homeless/src/AppBundle/Controller/AppController.php#L136 ?

        $searchVal = $this->getRequest()->query->get('v'); // TODO: deprecated in Symfony 3+, inject
        $clients = $this->getDoctrine()->getRepository('AppBundle:Client')
            ->search($searchVal);

        if (count($clients) === 0) {
            throw new NotFoundHttpException();
        }

        $view = $this->view($clients, 200);
        return $this->handleView($view);
    }

    /**
     * @param $id
     * @return \Symfony\Component\HttpFoundation\Response
     * @View(serializerGroups={"client"})
     */
    public function getClientAction($id)
    {
        $client = $this->getDoctrine()->getRepository('AppBundle:Client')->find($id);

        if (!$client instanceof Client) {
            throw new NotFoundHttpException('Client not found');
        }

        // TODO(now) serialization
        $view = $this->view($client, 200);
        return $this->handleView($view);
    }

    /**
     * @Route("/clients/{clientID}/humaiditem_delivery/{itemID}") // TODO: fix  app_api_clients_postclienthumaiditemdelivery     ANY        ANY      ANY    /clients/{clientID}/humaiditem_delivery/{itemID}
     * @param $clientID
     * @param $itemID
     * @return \Symfony\Component\HttpFoundation\Response
     * @View(serializerGroups={"client"})
     */
    public function postClientHumAidItemDeliveryAction($clientID, $itemID)
    {
        $em = $this->getDoctrine()->getManager();

        $client = $em->getRepository('AppBundle:Client')->find($clientID);
        if (!$client instanceof Client) {
            throw new NotFoundHttpException('Client not found');
        }

        $item = $em->getRepository('AppBundle:HumAidItem')->find($itemID);
        if (!$item instanceof HumAidItem) {
            throw new NotFoundHttpException('HumAidItem not found');
        }

        $delivery = (new HumAidItemDelivery())
            ->setClient($client)
            ->setHumAidItem($item)
            ->setDeliveredAt(new \DateTime());

        $em->persist($delivery);
        $em->flush();
    }

}
