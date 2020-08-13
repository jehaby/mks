<?php

namespace AppBundle\Controller\API;

use AppBundle\Entity\Client;
use AppBundle\Entity\HumAidItem;
use AppBundle\Entity\HumAidItemDelivery;
use Doctrine\ORM\NoResultException;
use Doctrine\ORM\Query;
use FOS\RestBundle\Controller\Annotations as Rest;
use FOS\RestBundle\Controller\Annotations\Route;
use FOS\RestBundle\Controller\Annotations\View;
use FOS\RestBundle\Controller\FOSRestController;
use Symfony\Component\HttpKernel\Exception\BadRequestHttpException;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;


class ClientsController extends FOSRestController
{

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
        $req = $this->getRequest();

        $em = $this->getDoctrine()->getEntityManager();
        $qb = $em->createQueryBuilder();

        try {
            $client = $qb
                    ->select(['c'])
                    ->from('AppBundle\Entity\Client', 'c')
                    ->where('c.id = :id')
                    ->setParameter('id', $id)
                    ->getQuery()
                    ->setHint(Query::HINT_FORCE_PARTIAL_LOAD, true)
                    ->getSingleResult(Query::HYDRATE_ARRAY);
        } catch (NoResultException $e) {
            throw new NotFoundHttpException('Client not found');
        }

        $view = $this->view($client, 200);
        return $this->handleView($view);
    }

    /**
     * @Route("/clients/{clientID}/humaiditem_deliveries")
     * TODO: better api docs
     * @param $clientID
     * @return \Symfony\Component\HttpFoundation\Response
     */
    public function getClientHumAidItemDeliveriesAction($clientID)
    {
        $this->checkClientExists($clientID);

        $items = $this
               ->getDoctrine()
               ->getEntityManager()
               ->createQueryBuilder()
               ->select('MAX(hid.deliveredAt) AS deliveredAt, IDENTITY(hid.humAidItem) AS humAidItemID')
               ->from('AppBundle\Entity\HumAidItemDelivery', 'hid')
               ->andWhere('IDENTITY(hid.client) = :cid')
               ->groupBy('hid.humAidItem')
               ->setParameter('cid', $clientID)
               ->getQuery()
               ->getArrayResult();

        $view = $this->view($items, 200);
        return $this->handleView($view);
    }

    /**
     * @Route("/clients/{clientID}/services")
     * @Rest\QueryParam(name="types", description="array of service type ids")
     * TODO: better api docs
     * @param $clientID
     * @return \Symfony\Component\HttpFoundation\Response
     */
    public function getClientServicesAction($clientID)
    {
        $req = $this->getRequest();
        $types = $req->query->get('types');

        $this->checkClientExists($clientID);

        $qb = $this->getDoctrine()->getEntityManager()->createQueryBuilder();

        $services = $qb
                  ->select('s.comment, s.amount, IDENTITY(s.type) AS type, s.createdAt, s.updatedAt')
                  ->from('AppBundle\Entity\Service', 's')
                  ->andWhere('IDENTITY(s.client) = :cid')
                  ->andWhere($qb->expr()->in('IDENTITY(s.type)', ':types'))
                  ->setParameter('cid', $clientID)
                  ->setParameter('types', $types)
                  ->orderBy('s.createdAt', 'DESC')
                  ->getQuery()
                  ->getArrayResult();

        $view = $this->view($services, 200);
        return $this->handleView($view);
    }

    private function checkClientExists($clientID)
    {
        /** @var Doctrine\ORM\Query $query */
        $query = $this->getDoctrine()->getEntityManager()
               ->createQuery('SELECT 1 FROM AppBundle\Entity\Client c WHERE c.id = :cid')
               ->setParameter('cid', $clientID)
               ->setMaxResults(1);

        if (count($query->getResult()) === 0) {
            throw new NotFoundHttpException('Client not found');
        }
    }

    /**
     * @Route("/clients/{clientID}/humaiditem_deliveries")
     * TODO: better api docs
     * @param $clientID
     * @return \Symfony\Component\HttpFoundation\Response
     * @View(serializerGroups={"client"})
     */
    public function postClientHumAidItemDeliveriesAction($clientID)
    {
        $em = $this->getDoctrine()->getManager();

        $data = json_decode($this->getRequest()->getContent(), true);

        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new BadRequestHttpException('invalid json body: ' . json_last_error_msg());
        }

        $client = $em->getRepository('AppBundle:Client')->find($clientID);
        if (!$client instanceof Client) {
            throw new NotFoundHttpException('Client not found');
        }

        $items = $em->getRepository('AppBundle:HumAidItem')->findById($data['item_ids']);

        //        TODO: better errors

        foreach ($items as $item) {
            $delivery = (new HumAidItemDelivery())
                      ->setClient($client)
                      ->setHumAidItem($item)
                      ->setDeliveredAt(new \DateTime());

            $em->persist($delivery);
        }

        $em->flush();
    }

}
